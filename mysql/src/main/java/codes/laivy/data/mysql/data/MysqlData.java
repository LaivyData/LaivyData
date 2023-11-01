package codes.laivy.data.mysql.data;

import codes.laivy.data.data.Data;
import codes.laivy.data.mysql.MysqlDatabase;
import codes.laivy.data.mysql.table.MysqlTable;
import codes.laivy.data.mysql.utils.SqlUtils;
import codes.laivy.data.mysql.variable.Parameter;
import codes.laivy.data.mysql.variable.Type;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class MysqlData extends Data {

    // Static methods

    private static final @NotNull Map<@NotNull MysqlTable, @NotNull List<@NotNull MysqlData>> CACHED_LOADED_DATAS = new HashMap<>();

    // TODO: 01/11/2023 This
//    public static @NotNull CompletableFuture<Data> retrieve(@NotNull MysqlTable table, long row) {
//
//    }
    public static @NotNull CompletableFuture<Data[]> retrieve(@NotNull MysqlTable table, @NotNull Condition<?> @NotNull ... conditions) {
        @Nullable Connection connection = table.getDatabase().getAuthentication().getConnection();

        if (conditions.length == 0) {
            throw new IllegalStateException("The conditions array cannot be empty");
        } else if (connection == null) {
            throw new IllegalStateException("The table's authentication aren't connected");
        } else if (!table.isLoaded() || !table.getDatabase().isLoaded()) {
            throw new IllegalStateException("This table or database aren't loaded");
        } else if (Arrays.stream(conditions).anyMatch(c -> !c.getVariable().getTable().equals(table))) {
            throw new IllegalStateException("There's conditions with variables that aren't from the table '" + table.getName() + "'");
        } else if (Arrays.stream(conditions).anyMatch(c -> !c.getVariable().isLoaded())) {
            throw new IllegalStateException("There's conditions with variables that hasn't loaded");
        }

        // TODO: 01/11/2023 Verificar se há dois conditions com a mesma variável

        @NotNull CompletableFuture<Data[]> future = new CompletableFuture<>();

        CompletableFuture.runAsync(() -> {
            try {
                Map<Long, MysqlData> datas = new TreeMap<>(Long::compare);

                if (CACHED_LOADED_DATAS.containsKey(table)) {
                    for (MysqlData data : CACHED_LOADED_DATAS.get(table).stream().filter(data -> data.isLoaded() && data.matches(conditions)).collect(Collectors.toList())) {
                        datas.put(data.getRow(), data);
                    }
                }

                // Retrieving on database

                try (PreparedStatement statement = connection.prepareStatement("SELECT `row` FROM `" + table.getDatabase().getId() + "`.`" + table.getName() + "` " + SqlUtils.buildWhereCondition(datas.keySet(), conditions))) {
                    int index = 0;
                    for (@NotNull Condition<?> condition : conditions) {
                        //noinspection rawtypes
                        @NotNull Type type = condition.getVariable().getType();
                        //noinspection unchecked
                        type.set(Parameter.of(statement, type.isNullSupported(), index), condition.getValue());
                        index++;
                    }

                    @NotNull ResultSet set = statement.executeQuery();
                    while (set.next()) {
                        long row = set.getInt("row");
                        @NotNull MysqlData data = new MysqlData(table, row);
                        CACHED_LOADED_DATAS.computeIfAbsent(table, k -> new ArrayList<>()).add(data);

                        if (!datas.containsKey(row)) {
                            datas.put(row, data);
                        }
                    }
                }

                future.complete(datas.values().toArray(new Data[0]));
            } catch (@NotNull Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });

        return future;
    }

    // Object

    private final @NotNull MysqlTable table;
    private long row;

    private MysqlData(@NotNull MysqlTable table, long row) {
        this.table = table;
        this.row = row;
    }

    public final long getRow() {
        return row;
    }

    // TODO: 01/11/2023 setRow

    @Contract(pure = true)
    public final @NotNull MysqlDatabase getDatabase() {
        return getTable().getDatabase();
    }

    @Contract(pure = true)
    public final @NotNull MysqlTable getTable() {
        return table;
    }

    @Override
    public @Nullable Object get(@NotNull String id) {
        return null;
    }

    @Override
    public void set(@NotNull String id, @Nullable Object object) {

    }

    @Override
    protected @NotNull CompletableFuture<Void> load() {
        return null;
    }

    @Override
    protected @NotNull CompletableFuture<Void> unload(boolean save) {
        return null;
    }

    @Override
    public @NotNull CompletableFuture<Void> save() {
        return null;
    }

    public boolean matches(@NotNull Condition<?> @NotNull [] conditions) {
        if (!isLoaded()) {
            throw new IllegalStateException("The mysql data must be loaded to use the #matches");
        } else if (Arrays.stream(conditions).anyMatch(c -> !c.getVariable().getTable().equals(getTable()))) {
            throw new IllegalStateException("There's conditions with variables that aren't from this data");
        } else if (Arrays.stream(conditions).anyMatch(c -> !c.getVariable().isLoaded())) {
            throw new IllegalStateException("There's conditions with variables that hasn't loaded");
        } else for (@NotNull Condition<?> condition : conditions) {
            if (!Objects.equals(get(condition.getVariable().getId()), condition.getValue())) {
                return false;
            }
        }

        return true;
    }

}
