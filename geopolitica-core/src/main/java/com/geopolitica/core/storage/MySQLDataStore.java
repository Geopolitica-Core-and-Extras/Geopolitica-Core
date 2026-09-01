package com.geopolitica.core.storage;

import com.geopolitica.api.claim.ClaimPermission;
import com.geopolitica.api.claim.TrustLevel;
import com.geopolitica.api.nation.DiplomaticStatus;
import com.geopolitica.api.town.TownPermission;
import com.geopolitica.core.config.ConfigManager;
import com.geopolitica.core.model.ClaimImpl;
import com.geopolitica.core.model.NationImpl;
import com.geopolitica.core.model.ResidentImpl;
import com.geopolitica.core.model.StateImpl;
import com.geopolitica.core.model.TownImpl;
import com.geopolitica.core.model.TownRankImpl;
import com.geopolitica.core.util.ChunkPos;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.awt.Color;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * MySQL/MariaDB-backed implementation of {@link DataStore}, for shared storage across a
 * multi-server network. Schema and upsert logic mirror {@link SQLiteDataStore} field-for-field;
 * kept as a separate class rather than behind a shared abstraction because the two dialects
 * differ enough (DDL types, upsert syntax, the column-introspection query) that a shared base
 * would just push per-dialect branches into every method.
 *
 * <p>Unlike SQLite's forced single-connection pool, MySQL genuinely serves concurrent
 * connections, so writes here go through {@link #inTransaction} which retries once on an
 * InnoDB deadlock (SQLState 40001) - a well-known hazard of concurrent upsert-then-rewrite
 * transactions on the same row, which two {@code saveTown} calls for the same town landing
 * close together can trigger.
 */
public class MySQLDataStore implements DataStore {

    private static final String[] SCHEMA = {
            """
            CREATE TABLE IF NOT EXISTS towns (
                id VARCHAR(36) PRIMARY KEY,
                name VARCHAR(64) UNIQUE NOT NULL,
                owner_id VARCHAR(36) NOT NULL,
                max_claims INT NOT NULL,
                bank_balance DOUBLE NOT NULL DEFAULT 0,
                description TEXT NOT NULL,
                map_color INT NOT NULL DEFAULT -16711936,
                home_world VARCHAR(255),
                home_x DOUBLE, home_y DOUBLE, home_z DOUBLE, home_yaw FLOAT, home_pitch FLOAT,
                open TINYINT(1) NOT NULL DEFAULT 0,
                pvp TINYINT(1) NOT NULL DEFAULT 0,
                frozen TINYINT(1) NOT NULL DEFAULT 0,
                state_id VARCHAR(36),
                direct_nation_id VARCHAR(36)
            ) ENGINE=InnoDB""",
            """
            CREATE TABLE IF NOT EXISTS town_ranks (
                town_id VARCHAR(36) NOT NULL,
                name VARCHAR(64) NOT NULL,
                is_owner_rank TINYINT(1) NOT NULL DEFAULT 0,
                is_default_rank TINYINT(1) NOT NULL DEFAULT 0,
                PRIMARY KEY (town_id, name),
                FOREIGN KEY (town_id) REFERENCES towns(id)
            ) ENGINE=InnoDB""",
            """
            CREATE TABLE IF NOT EXISTS rank_permissions (
                town_id VARCHAR(36) NOT NULL,
                rank_name VARCHAR(64) NOT NULL,
                permission VARCHAR(64) NOT NULL,
                PRIMARY KEY (town_id, rank_name, permission)
            ) ENGINE=InnoDB""",
            """
            CREATE TABLE IF NOT EXISTS residents (
                id VARCHAR(36) PRIMARY KEY,
                name VARCHAR(64) NOT NULL,
                town_id VARCHAR(36),
                rank_name VARCHAR(64),
                last_seen BIGINT NOT NULL DEFAULT 0
            ) ENGINE=InnoDB""",
            """
            CREATE TABLE IF NOT EXISTS claims (
                world VARCHAR(255) NOT NULL,
                chunk_x INT NOT NULL,
                chunk_z INT NOT NULL,
                town_id VARCHAR(36) NOT NULL,
                plot_name VARCHAR(64) NOT NULL DEFAULT '',
                PRIMARY KEY (world, chunk_x, chunk_z)
            ) ENGINE=InnoDB""",
            """
            CREATE TABLE IF NOT EXISTS claim_permissions (
                world VARCHAR(255) NOT NULL,
                chunk_x INT NOT NULL,
                chunk_z INT NOT NULL,
                trust_level VARCHAR(32) NOT NULL,
                permission VARCHAR(32) NOT NULL,
                PRIMARY KEY (world, chunk_x, chunk_z, trust_level, permission)
            ) ENGINE=InnoDB""",
            """
            CREATE TABLE IF NOT EXISTS nations (
                id VARCHAR(36) PRIMARY KEY,
                name VARCHAR(64) UNIQUE NOT NULL,
                capital_town_id VARCHAR(36) NOT NULL,
                bank_balance DOUBLE NOT NULL DEFAULT 0,
                description TEXT NOT NULL,
                map_color INT NOT NULL DEFAULT -16776961,
                open TINYINT(1) NOT NULL DEFAULT 0,
                frozen TINYINT(1) NOT NULL DEFAULT 0
            ) ENGINE=InnoDB""",
            """
            CREATE TABLE IF NOT EXISTS states (
                id VARCHAR(36) PRIMARY KEY,
                name VARCHAR(64) UNIQUE NOT NULL,
                nation_id VARCHAR(36) NOT NULL,
                capital_town_id VARCHAR(36) NOT NULL,
                leader_id VARCHAR(36),
                bank_balance DOUBLE NOT NULL DEFAULT 0,
                description TEXT NOT NULL,
                map_color INT NOT NULL DEFAULT -16776961,
                open TINYINT(1) NOT NULL DEFAULT 0,
                frozen TINYINT(1) NOT NULL DEFAULT 0
            ) ENGINE=InnoDB""",
            """
            CREATE TABLE IF NOT EXISTS nation_relations (
                nation_a VARCHAR(36) NOT NULL,
                nation_b VARCHAR(36) NOT NULL,
                status VARCHAR(32) NOT NULL,
                PRIMARY KEY (nation_a, nation_b)
            ) ENGINE=InnoDB"""
    };

    private static final String MYSQL_DEADLOCK_SQLSTATE = "40001";

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private HikariDataSource dataSource;

    public MySQLDataStore(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    @Override
    public void init() throws Exception {
        HikariConfig hikariConfig = new HikariConfig();
        String url = "jdbc:mysql://" + configManager.getMysqlHost() + ":" + configManager.getMysqlPort()
                + "/" + configManager.getMysqlDatabase()
                + "?useSSL=" + configManager.getMysqlUseSsl()
                + "&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=UTF-8";
        hikariConfig.setJdbcUrl(url);
        hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        hikariConfig.setUsername(configManager.getMysqlUsername());
        hikariConfig.setPassword(configManager.getMysqlPassword());
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setPoolName("Geopolitica-MySQL");
        this.dataSource = new HikariDataSource(hikariConfig);

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            for (String ddl : SCHEMA) {
                statement.execute(ddl);
            }
        }
    }

    @Override
    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Override
    public LoadResult loadAll() throws Exception {
        Map<UUID, ResidentImpl> residents = new LinkedHashMap<>();
        Map<UUID, TownImpl> towns = new LinkedHashMap<>();
        Map<ChunkPos, ClaimImpl> claims = new LinkedHashMap<>();

        Map<UUID, NationImpl> nations = new LinkedHashMap<>();
        Map<UUID, StateImpl> states = new LinkedHashMap<>();
        Map<UUID, Map<UUID, DiplomaticStatus>> relations = new LinkedHashMap<>();

        // Residents (town/rank linkage recorded but applied after towns exist)
        Map<UUID, UUID> residentTownIds = new LinkedHashMap<>();
        Map<UUID, String> residentRankNames = new LinkedHashMap<>();

        // Town hierarchy linkage recorded but applied after nations/states exist
        Map<UUID, UUID> townStateIds = new LinkedHashMap<>();
        Map<UUID, UUID> townDirectNationIds = new LinkedHashMap<>();

        try (Connection connection = dataSource.getConnection()) {
            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery("SELECT id, name, town_id, rank_name, last_seen FROM residents")) {
                while (rs.next()) {
                    UUID id = UUID.fromString(rs.getString("id"));
                    ResidentImpl resident = new ResidentImpl(id, rs.getString("name"));
                    resident.setLastSeen(rs.getLong("last_seen"));
                    residents.put(id, resident);

                    String townId = rs.getString("town_id");
                    if (townId != null) {
                        residentTownIds.put(id, UUID.fromString(townId));
                        residentRankNames.put(id, rs.getString("rank_name"));
                    }
                }
            }

            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery(
                         "SELECT id, name, owner_id, max_claims, bank_balance, description, map_color, "
                                 + "home_world, home_x, home_y, home_z, home_yaw, home_pitch, open, pvp, frozen, "
                                 + "state_id, direct_nation_id FROM towns")) {
                while (rs.next()) {
                    UUID id = UUID.fromString(rs.getString("id"));
                    TownImpl town = TownImpl.rehydrate(id, rs.getString("name"), rs.getInt("max_claims"));
                    town.depositBank(rs.getDouble("bank_balance"));
                    town.setDescription(rs.getString("description"));
                    town.setMapColor(new Color(rs.getInt("map_color")));
                    town.setOpen(rs.getInt("open") != 0);
                    town.setPvpEnabled(rs.getInt("pvp") != 0);
                    town.setFrozen(rs.getInt("frozen") != 0);

                    // residents is already fully populated above, so the owner can be
                    // resolved here instead of a second `SELECT id, owner_id FROM towns` pass.
                    ResidentImpl owner = residents.get(UUID.fromString(rs.getString("owner_id")));
                    if (owner != null) {
                        town.restoreOwner(owner);
                    } else {
                        plugin.getLogger().severe("Town '" + town.getName() + "' references a missing owner resident; "
                                + "it will have no owner until fixed manually.");
                    }

                    String stateId = rs.getString("state_id");
                    if (stateId != null) {
                        townStateIds.put(id, UUID.fromString(stateId));
                    }
                    String directNationId = rs.getString("direct_nation_id");
                    if (directNationId != null) {
                        townDirectNationIds.put(id, UUID.fromString(directNationId));
                    }

                    String homeWorld = rs.getString("home_world");
                    if (homeWorld != null) {
                        World world = Bukkit.getWorld(homeWorld);
                        if (world != null) {
                            town.setHomeLocation(new Location(world,
                                    rs.getDouble("home_x"), rs.getDouble("home_y"), rs.getDouble("home_z"),
                                    rs.getFloat("home_yaw"), rs.getFloat("home_pitch")));
                        } else {
                            plugin.getLogger().warning("Town '" + town.getName() + "' has a home in unknown world '"
                                    + homeWorld + "'; skipping until that world loads.");
                        }
                    }
                    towns.put(id, town);
                }
            }

            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery("SELECT town_id, name, is_owner_rank, is_default_rank FROM town_ranks")) {
                while (rs.next()) {
                    TownImpl town = towns.get(UUID.fromString(rs.getString("town_id")));
                    if (town == null) {
                        continue;
                    }
                    TownRankImpl rank = new TownRankImpl(rs.getString("name"));
                    rank.setOwnerRank(rs.getInt("is_owner_rank") != 0);
                    rank.setDefaultRank(rs.getInt("is_default_rank") != 0);
                    town.restoreRank(rank);
                }
            }

            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery("SELECT town_id, rank_name, permission FROM rank_permissions")) {
                while (rs.next()) {
                    TownImpl town = towns.get(UUID.fromString(rs.getString("town_id")));
                    if (town == null) {
                        continue;
                    }
                    TownRankImpl rank = town.getRankImpl(rs.getString("rank_name"));
                    if (rank == null) {
                        continue;
                    }
                    rank.setPermission(TownPermission.valueOf(rs.getString("permission")), true);
                }
            }

            for (Map.Entry<UUID, UUID> entry : residentTownIds.entrySet()) {
                ResidentImpl resident = residents.get(entry.getKey());
                TownImpl town = towns.get(entry.getValue());
                if (town == null) {
                    continue;
                }
                String rankName = residentRankNames.get(entry.getKey());
                TownRankImpl rank = rankName != null ? town.getRankImpl(rankName) : null;
                if (rank == null) {
                    rank = town.getDefaultRankImpl();
                }
                town.restoreResident(resident, rank);
            }

            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery("SELECT world, chunk_x, chunk_z, town_id, plot_name FROM claims")) {
                while (rs.next()) {
                    TownImpl town = towns.get(UUID.fromString(rs.getString("town_id")));
                    if (town == null) {
                        continue;
                    }
                    String world = rs.getString("world");
                    int x = rs.getInt("chunk_x");
                    int z = rs.getInt("chunk_z");
                    ClaimImpl claim = new ClaimImpl(town, world, x, z);
                    claim.setPlotName(rs.getString("plot_name"));
                    town.addClaim(claim);
                    claims.put(new ChunkPos(world, x, z), claim);
                }
            }

            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery("SELECT world, chunk_x, chunk_z, trust_level, permission FROM claim_permissions")) {
                while (rs.next()) {
                    ChunkPos pos = new ChunkPos(rs.getString("world"), rs.getInt("chunk_x"), rs.getInt("chunk_z"));
                    ClaimImpl claim = claims.get(pos);
                    if (claim == null) {
                        continue;
                    }
                    claim.setPermission(
                            TrustLevel.valueOf(rs.getString("trust_level")),
                            ClaimPermission.valueOf(rs.getString("permission")),
                            true);
                }
            }

            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery(
                         "SELECT id, name, capital_town_id, bank_balance, description, map_color, open, frozen FROM nations")) {
                while (rs.next()) {
                    TownImpl capital = towns.get(UUID.fromString(rs.getString("capital_town_id")));
                    if (capital == null) {
                        continue;
                    }
                    UUID id = UUID.fromString(rs.getString("id"));
                    NationImpl nation = new NationImpl(id, rs.getString("name"), capital);
                    nation.depositBank(rs.getDouble("bank_balance"));
                    nation.setDescription(rs.getString("description"));
                    nation.setMapColor(new Color(rs.getInt("map_color")));
                    nation.setOpen(rs.getInt("open") != 0);
                    nation.setFrozen(rs.getInt("frozen") != 0);
                    nations.put(id, nation);
                }
            }

            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery(
                         "SELECT id, name, nation_id, capital_town_id, leader_id, bank_balance, description, map_color, open, frozen FROM states")) {
                while (rs.next()) {
                    NationImpl nation = nations.get(UUID.fromString(rs.getString("nation_id")));
                    TownImpl capital = towns.get(UUID.fromString(rs.getString("capital_town_id")));
                    if (nation == null || capital == null) {
                        continue;
                    }
                    UUID id = UUID.fromString(rs.getString("id"));
                    StateImpl state = new StateImpl(id, rs.getString("name"), nation, capital);
                    String leaderId = rs.getString("leader_id");
                    if (leaderId != null) {
                        ResidentImpl leader = residents.get(UUID.fromString(leaderId));
                        if (leader != null) {
                            state.restoreLeader(leader);
                        }
                    }
                    state.depositBank(rs.getDouble("bank_balance"));
                    state.setDescription(rs.getString("description"));
                    state.setMapColor(new Color(rs.getInt("map_color")));
                    state.setOpen(rs.getInt("open") != 0);
                    state.setFrozen(rs.getInt("frozen") != 0);
                    states.put(id, state);
                    nation.getStateImpls().add(state);
                }
            }

            for (Map.Entry<UUID, UUID> entry : townStateIds.entrySet()) {
                TownImpl town = towns.get(entry.getKey());
                StateImpl state = states.get(entry.getValue());
                if (town == null || state == null) {
                    continue;
                }
                town.setStateImpl(state);
                if (state.getCapitalImpl() != town) {
                    state.getTownImpls().add(town);
                }
            }
            for (Map.Entry<UUID, UUID> entry : townDirectNationIds.entrySet()) {
                TownImpl town = towns.get(entry.getKey());
                NationImpl nation = nations.get(entry.getValue());
                if (town == null || nation == null || town.getStateImpl() != null) {
                    continue;
                }
                town.setDirectNationImpl(nation);
                if (nation.getCapitalImpl() != town) {
                    nation.getDirectTownImpls().add(town);
                }
            }

            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery("SELECT nation_a, nation_b, status FROM nation_relations")) {
                while (rs.next()) {
                    UUID a = UUID.fromString(rs.getString("nation_a"));
                    UUID b = UUID.fromString(rs.getString("nation_b"));
                    DiplomaticStatus status = DiplomaticStatus.valueOf(rs.getString("status"));
                    if (!nations.containsKey(a) || !nations.containsKey(b)) {
                        continue;
                    }
                    relations.computeIfAbsent(a, k -> new LinkedHashMap<>()).put(b, status);
                    relations.computeIfAbsent(b, k -> new LinkedHashMap<>()).put(a, status);
                }
            }
        }

        return new LoadResult(towns, residents, claims, nations, states, relations);
    }

    @Override
    public void saveTown(TownImpl town) {
        inTransaction("save town " + town.getName(), connection -> {
            Location home = town.getHomeLocation().orElse(null);

            // Plain `INSERT ... ON DUPLICATE KEY UPDATE` would match on ANY unique key, not just
            // `id` - since `name` is also unique here, saving a town whose name collides with a
            // different existing row would silently overwrite that other row instead of this
            // one. Update-by-id-first (falling back to insert) scopes the upsert to `id` only,
            // matching SQLite's `ON CONFLICT(id)` semantics exactly.
            boolean updated;
            try (PreparedStatement ps = connection.prepareStatement("""
                    UPDATE towns SET name = ?, owner_id = ?, max_claims = ?, bank_balance = ?, description = ?,
                        map_color = ?, home_world = ?, home_x = ?, home_y = ?, home_z = ?, home_yaw = ?, home_pitch = ?,
                        open = ?, pvp = ?, frozen = ?, state_id = ?, direct_nation_id = ?
                    WHERE id = ?
                    """)) {
                bindTownUpsertColumns(ps, town, home);
                ps.setString(18, town.getUniqueId().toString());
                updated = ps.executeUpdate() > 0;
            }
            if (!updated) {
                try (PreparedStatement ps = connection.prepareStatement("""
                        INSERT INTO towns (name, owner_id, max_claims, bank_balance, description, map_color,
                            home_world, home_x, home_y, home_z, home_yaw, home_pitch, open, pvp, frozen,
                            state_id, direct_nation_id, id)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """)) {
                    bindTownUpsertColumns(ps, town, home);
                    ps.setString(18, town.getUniqueId().toString());
                    ps.executeUpdate();
                }
            }

            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM rank_permissions WHERE town_id = ?")) {
                ps.setString(1, town.getUniqueId().toString());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM town_ranks WHERE town_id = ?")) {
                ps.setString(1, town.getUniqueId().toString());
                ps.executeUpdate();
            }
            try (PreparedStatement rankPs = connection.prepareStatement(
                    "INSERT INTO town_ranks (town_id, name, is_owner_rank, is_default_rank) VALUES (?, ?, ?, ?)");
                 PreparedStatement permPs = connection.prepareStatement(
                         "INSERT INTO rank_permissions (town_id, rank_name, permission) VALUES (?, ?, ?)")) {
                for (TownRankImpl rank : town.getRankImpls()) {
                    rankPs.setString(1, town.getUniqueId().toString());
                    rankPs.setString(2, rank.getName());
                    rankPs.setInt(3, rank.isOwnerRank() ? 1 : 0);
                    rankPs.setInt(4, rank.isDefaultRank() ? 1 : 0);
                    rankPs.executeUpdate();

                    if (!rank.isOwnerRank()) {
                        for (TownPermission permission : rank.getPermissions()) {
                            permPs.setString(1, town.getUniqueId().toString());
                            permPs.setString(2, rank.getName());
                            permPs.setString(3, permission.name());
                            permPs.executeUpdate();
                        }
                    }
                }
            }
        });
    }

    /** Binds the 17 non-id town columns, in the shared column order both the UPDATE and INSERT in {@link #saveTown} use. */
    private void bindTownUpsertColumns(PreparedStatement ps, TownImpl town, Location home) throws SQLException {
        ps.setString(1, town.getName());
        ps.setString(2, town.getOwner().getUniqueId().toString());
        ps.setInt(3, town.getMaxClaims());
        ps.setDouble(4, town.getBankBalance());
        ps.setString(5, town.getDescription());
        ps.setInt(6, town.getMapColor().getRGB());
        if (home != null) {
            ps.setString(7, home.getWorld().getName());
            ps.setDouble(8, home.getX());
            ps.setDouble(9, home.getY());
            ps.setDouble(10, home.getZ());
            ps.setFloat(11, home.getYaw());
            ps.setFloat(12, home.getPitch());
        } else {
            ps.setNull(7, Types.VARCHAR);
            ps.setNull(8, Types.DOUBLE);
            ps.setNull(9, Types.DOUBLE);
            ps.setNull(10, Types.DOUBLE);
            ps.setNull(11, Types.FLOAT);
            ps.setNull(12, Types.FLOAT);
        }
        ps.setInt(13, town.isOpen() ? 1 : 0);
        ps.setInt(14, town.isPvpEnabled() ? 1 : 0);
        ps.setInt(15, town.isFrozen() ? 1 : 0);
        if (town.getStateImpl() != null) {
            ps.setString(16, town.getStateImpl().getUniqueId().toString());
        } else {
            ps.setNull(16, Types.VARCHAR);
        }
        if (town.getDirectNationImpl() != null) {
            ps.setString(17, town.getDirectNationImpl().getUniqueId().toString());
        } else {
            ps.setNull(17, Types.VARCHAR);
        }
    }

    @Override
    public void deleteTown(UUID townId) {
        String id = townId.toString();
        inTransaction("delete town " + id, connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM claim_permissions WHERE (world, chunk_x, chunk_z) IN "
                            + "(SELECT world, chunk_x, chunk_z FROM claims WHERE town_id = ?)")) {
                ps.setString(1, id);
                ps.executeUpdate();
            }
            execUpdate(connection, "DELETE FROM claims WHERE town_id = ?", id);
            execUpdate(connection, "UPDATE residents SET town_id = NULL, rank_name = NULL WHERE town_id = ?", id);
            execUpdate(connection, "DELETE FROM rank_permissions WHERE town_id = ?", id);
            execUpdate(connection, "DELETE FROM town_ranks WHERE town_id = ?", id);
            execUpdate(connection, "DELETE FROM towns WHERE id = ?", id);
        });
    }

    @Override
    public void saveResident(ResidentImpl resident) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("""
                     INSERT INTO residents (id, name, town_id, rank_name, last_seen)
                     VALUES (?, ?, ?, ?, ?)
                     ON DUPLICATE KEY UPDATE
                         name = VALUES(name), town_id = VALUES(town_id),
                         rank_name = VALUES(rank_name), last_seen = VALUES(last_seen)
                     """)) {
            // Safe as a plain ON DUPLICATE KEY UPDATE: residents has no unique key besides `id`.
            ps.setString(1, resident.getUniqueId().toString());
            ps.setString(2, resident.getName());
            if (resident.getTownImpl() != null) {
                ps.setString(3, resident.getTownImpl().getUniqueId().toString());
                ps.setString(4, resident.getRankImpl() != null ? resident.getRankImpl().getName() : null);
            } else {
                ps.setNull(3, Types.VARCHAR);
                ps.setNull(4, Types.VARCHAR);
            }
            ps.setLong(5, resident.getLastSeen());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save resident " + resident.getName(), e);
        }
    }

    @Override
    public void saveClaim(ClaimImpl claim) {
        inTransaction("save claim " + claim.getWorldName() + " " + claim.getChunkX() + "," + claim.getChunkZ(), connection -> {
            // Safe as a plain ON DUPLICATE KEY UPDATE: claims has no unique key besides its
            // (world, chunk_x, chunk_z) primary key.
            try (PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO claims (world, chunk_x, chunk_z, town_id, plot_name)
                    VALUES (?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                        town_id = VALUES(town_id), plot_name = VALUES(plot_name)
                    """)) {
                ps.setString(1, claim.getWorldName());
                ps.setInt(2, claim.getChunkX());
                ps.setInt(3, claim.getChunkZ());
                ps.setString(4, claim.getTownImpl().getUniqueId().toString());
                ps.setString(5, claim.getPlotName());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM claim_permissions WHERE world = ? AND chunk_x = ? AND chunk_z = ?")) {
                ps.setString(1, claim.getWorldName());
                ps.setInt(2, claim.getChunkX());
                ps.setInt(3, claim.getChunkZ());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO claim_permissions (world, chunk_x, chunk_z, trust_level, permission) VALUES (?, ?, ?, ?, ?)")) {
                for (TrustLevel level : TrustLevel.values()) {
                    for (ClaimPermission permission : ClaimPermission.values()) {
                        if (!claim.isPermissionSet(level, permission)) {
                            continue;
                        }
                        ps.setString(1, claim.getWorldName());
                        ps.setInt(2, claim.getChunkX());
                        ps.setInt(3, claim.getChunkZ());
                        ps.setString(4, level.name());
                        ps.setString(5, permission.name());
                        ps.executeUpdate();
                    }
                }
            }
        });
    }

    @Override
    public void deleteClaim(String worldName, int chunkX, int chunkZ) {
        inTransaction("delete claim " + worldName + " " + chunkX + "," + chunkZ, connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM claim_permissions WHERE world = ? AND chunk_x = ? AND chunk_z = ?")) {
                ps.setString(1, worldName);
                ps.setInt(2, chunkX);
                ps.setInt(3, chunkZ);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM claims WHERE world = ? AND chunk_x = ? AND chunk_z = ?")) {
                ps.setString(1, worldName);
                ps.setInt(2, chunkX);
                ps.setInt(3, chunkZ);
                ps.executeUpdate();
            }
        });
    }

    @Override
    public void saveNation(NationImpl nation) {
        try (Connection connection = dataSource.getConnection()) {
            // Update-by-id-first, same reasoning as saveTown: `name` is also unique here, so a
            // plain ON DUPLICATE KEY UPDATE could match a different nation's row on a name clash.
            boolean updated;
            try (PreparedStatement ps = connection.prepareStatement("""
                    UPDATE nations SET name = ?, capital_town_id = ?, bank_balance = ?, description = ?,
                        map_color = ?, open = ?, frozen = ? WHERE id = ?
                    """)) {
                bindNationUpsertColumns(ps, nation);
                ps.setString(8, nation.getUniqueId().toString());
                updated = ps.executeUpdate() > 0;
            }
            if (!updated) {
                try (PreparedStatement ps = connection.prepareStatement("""
                        INSERT INTO nations (name, capital_town_id, bank_balance, description, map_color, open, frozen, id)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """)) {
                    bindNationUpsertColumns(ps, nation);
                    ps.setString(8, nation.getUniqueId().toString());
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save nation " + nation.getName(), e);
        }
    }

    private void bindNationUpsertColumns(PreparedStatement ps, NationImpl nation) throws SQLException {
        ps.setString(1, nation.getName());
        ps.setString(2, nation.getCapitalImpl().getUniqueId().toString());
        ps.setDouble(3, nation.getBankBalance());
        ps.setString(4, nation.getDescription());
        ps.setInt(5, nation.getMapColor().getRGB());
        ps.setInt(6, nation.isOpen() ? 1 : 0);
        ps.setInt(7, nation.isFrozen() ? 1 : 0);
    }

    @Override
    public void deleteNation(UUID nationId) {
        String id = nationId.toString();
        inTransaction("delete nation " + id, connection -> {
            execUpdate(connection, "DELETE FROM nation_relations WHERE nation_a = ? OR nation_b = ?", id, id);
            execUpdate(connection, "UPDATE towns SET state_id = NULL WHERE state_id IN (SELECT id FROM states WHERE nation_id = ?)", id);
            execUpdate(connection, "DELETE FROM states WHERE nation_id = ?", id);
            execUpdate(connection, "UPDATE towns SET direct_nation_id = NULL WHERE direct_nation_id = ?", id);
            execUpdate(connection, "DELETE FROM nations WHERE id = ?", id);
        });
    }

    @Override
    public void saveState(StateImpl state) {
        try (Connection connection = dataSource.getConnection()) {
            // Update-by-id-first, same reasoning as saveTown/saveNation.
            boolean updated;
            try (PreparedStatement ps = connection.prepareStatement("""
                    UPDATE states SET name = ?, nation_id = ?, capital_town_id = ?, leader_id = ?,
                        bank_balance = ?, description = ?, map_color = ?, open = ?, frozen = ? WHERE id = ?
                    """)) {
                bindStateUpsertColumns(ps, state);
                ps.setString(10, state.getUniqueId().toString());
                updated = ps.executeUpdate() > 0;
            }
            if (!updated) {
                try (PreparedStatement ps = connection.prepareStatement("""
                        INSERT INTO states (name, nation_id, capital_town_id, leader_id, bank_balance, description, map_color, open, frozen, id)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """)) {
                    bindStateUpsertColumns(ps, state);
                    ps.setString(10, state.getUniqueId().toString());
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save state " + state.getName(), e);
        }
    }

    private void bindStateUpsertColumns(PreparedStatement ps, StateImpl state) throws SQLException {
        ps.setString(1, state.getName());
        ps.setString(2, state.getNationImpl().getUniqueId().toString());
        ps.setString(3, state.getCapitalImpl().getUniqueId().toString());
        if (state.getLeaderImpl() != null) {
            ps.setString(4, state.getLeaderImpl().getUniqueId().toString());
        } else {
            ps.setNull(4, Types.VARCHAR);
        }
        ps.setDouble(5, state.getBankBalance());
        ps.setString(6, state.getDescription());
        ps.setInt(7, state.getMapColor().getRGB());
        ps.setInt(8, state.isOpen() ? 1 : 0);
        ps.setInt(9, state.isFrozen() ? 1 : 0);
    }

    @Override
    public void deleteState(UUID stateId) {
        String id = stateId.toString();
        inTransaction("delete state " + id, connection -> {
            execUpdate(connection, "UPDATE towns SET state_id = NULL WHERE state_id = ?", id);
            execUpdate(connection, "DELETE FROM states WHERE id = ?", id);
        });
    }

    @Override
    public void saveNationRelation(UUID nationA, UUID nationB, DiplomaticStatus status) {
        String a = nationA.toString();
        String b = nationB.toString();
        boolean swap = a.compareTo(b) > 0;
        String first = swap ? b : a;
        String second = swap ? a : b;
        try (Connection connection = dataSource.getConnection()) {
            if (status == DiplomaticStatus.NEUTRAL) {
                try (PreparedStatement ps = connection.prepareStatement(
                        "DELETE FROM nation_relations WHERE nation_a = ? AND nation_b = ?")) {
                    ps.setString(1, first);
                    ps.setString(2, second);
                    ps.executeUpdate();
                }
                return;
            }
            // Safe as a plain ON DUPLICATE KEY UPDATE: nation_relations has no unique key
            // besides its (nation_a, nation_b) primary key.
            try (PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO nation_relations (nation_a, nation_b, status) VALUES (?, ?, ?)
                    ON DUPLICATE KEY UPDATE status = VALUES(status)
                    """)) {
                ps.setString(1, first);
                ps.setString(2, second);
                ps.setString(3, status.name());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save nation relation " + a + "/" + b, e);
        }
    }

    @FunctionalInterface
    private interface SqlAction {
        void run(Connection connection) throws SQLException;
    }

    /**
     * Runs {@code action} inside a single transaction on its own connection, committing on
     * success and rolling back on failure. Retries once, on a fresh connection, if the failure
     * was an InnoDB deadlock (SQLState 40001) - a real hazard here since, unlike SQLite's
     * single-connection pool, two writes for the same row (e.g. two {@code saveTown} calls for
     * the same town landing close together) can now genuinely run concurrently.
     */
    private void inTransaction(String errorContext, SqlAction action) {
        SQLException failure = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try (Connection connection = dataSource.getConnection()) {
                connection.setAutoCommit(false);
                try {
                    action.run(connection);
                    connection.commit();
                    return;
                } catch (SQLException e) {
                    connection.rollback();
                    throw e;
                } finally {
                    connection.setAutoCommit(true);
                }
            } catch (SQLException e) {
                failure = e;
                if (attempt == 1 && MYSQL_DEADLOCK_SQLSTATE.equals(e.getSQLState())) {
                    continue;
                }
                break;
            }
        }
        plugin.getLogger().log(Level.SEVERE, "Failed to " + errorContext, failure);
    }

    private static void execUpdate(Connection connection, String sql, String... params) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setString(i + 1, params[i]);
            }
            ps.executeUpdate();
        }
    }
}
