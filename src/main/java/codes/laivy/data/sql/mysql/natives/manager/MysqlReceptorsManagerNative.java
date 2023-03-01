package codes.laivy.data.sql.mysql.natives.manager;

import codes.laivy.data.api.variable.container.ActiveVariableContainer;
import codes.laivy.data.api.variable.container.ActiveVariableContainerImpl;
import codes.laivy.data.api.variable.container.InactiveVariableContainerImpl;
import codes.laivy.data.sql.SqlVariable;
import codes.laivy.data.sql.manager.SqlReceptorsManager;
import codes.laivy.data.sql.mysql.MysqlReceptor;
import codes.laivy.data.sql.mysql.connection.MysqlConnection;
import codes.laivy.data.sql.mysql.natives.MysqlReceptorNative;
import codes.laivy.data.sql.mysql.values.MysqlResultData;
import codes.laivy.data.sql.mysql.values.MysqlResultStatement;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

@ApiStatus.Internal
public class MysqlReceptorsManagerNative implements SqlReceptorsManager<MysqlReceptor> {

    private final @NotNull MysqlConnection connection;

    public MysqlReceptorsManagerNative(@NotNull MysqlConnection connection) {
        this.connection = connection;
    }

    public @NotNull MysqlConnection getConnection() {
        return connection;
    }

    @Override
    public @Nullable MysqlResultData getData(@NotNull MysqlReceptor receptor) {
        MysqlResultStatement statement = getConnection().createStatement("SELECT * FROM `" + receptor.getDatabase().getId() + "`.`" + receptor.getTable().getId() + "` WHERE id = ?");
        statement.getParameters(0).setString(receptor.getId());
        MysqlResultData query = statement.execute();
        statement.close();

        if (query == null) {
            throw new NullPointerException("This result data doesn't have results");
        }

        @NotNull Set<Map<String, Object>> results = query.getValues();
        query.close();

        if (results.isEmpty()) {
            return null;
        } else if (results.size() == 1) {
            return query;
        } else {
            throw new UnsupportedOperationException("Multiples receptors with same id '" + receptor.getId() + "' founded inside table '" + receptor.getTable().getId() + "' at database '" + receptor.getDatabase().getId() + "'");
        }
    }

    @Override
    public void unload(@NotNull MysqlReceptor receptor, boolean save) {
        if (save) {
            save(receptor);
        }
    }

    @Override
    public void save(@NotNull MysqlReceptor receptor) {
        StringBuilder query = new StringBuilder();

        Map<Integer, ActiveVariableContainer> indexVariables = new LinkedHashMap<>();

        int row = 0;
        for (ActiveVariableContainer activeVar : receptor.getActiveContainers()) {
            if (row != 0) query.append(",");
            query.append("``=?");
            indexVariables.put(row, activeVar);
            row++;
        }

        MysqlResultStatement statement = getConnection().createStatement("UPDATE `" + receptor.getDatabase().getId() + "`.`" + receptor.getTable().getId() + "` SET " + query + " WHERE id = ?");
        statement.getParameters(0).setString(receptor.getId());

        for (Map.Entry<Integer, ActiveVariableContainer> map : indexVariables.entrySet()) {
            ((SqlVariable) map.getValue().getVariable()).getType().set(
                    map.getValue().get(),
                    statement.getParameters(map.getKey()),
                    statement.getMetaData()
            );
        }
        statement.execute();
        statement.close();
    }

    @Override
    public void delete(@NotNull MysqlReceptor receptor) {
        MysqlResultStatement statement = getConnection().createStatement("DELETE FROM `" + receptor.getDatabase().getId() + "`.`" + receptor.getTable().getId() + "` WHERE id = ?");
        statement.getParameters(0).setString(receptor.getId());
        statement.execute();
        statement.close();
    }

    @Override
    public void load(MysqlReceptor receptor) {
        @Nullable MysqlResultData result = getData(receptor);
        Map<String, Object> data = new LinkedHashMap<>();
        if (result != null) {
            data = new LinkedList<>(result.getValues()).getFirst();
            result.close();
        }

        if (receptor instanceof MysqlReceptorNative) {
            ((MysqlReceptorNative) receptor).setNew(data.isEmpty());
        } else {
            throw new IllegalStateException("The native mysql manager's receptor manager doesn't supports that receptor type!");
        }

        if (data.isEmpty()) {
            // Execute
            MysqlResultStatement statement = getConnection().createStatement("INSERT INTO `" + receptor.getDatabase().getId() + "`.`" + receptor.getTable().getId() + "` (id) VALUES (?)");
            statement.getParameters(0).setString(receptor.getId());
            statement.execute();
            statement.close();
            // Data query (again)
            result = getData(receptor);
            if (result != null) {
                data = new LinkedList<>(result.getValues()).getFirst();
                result.close();
            } else {
                throw new NullPointerException("Couldn't create receptor '" + receptor.getId() + "' due to an unknown error.");
            }
        }

        int row = 0;
        for (Map.Entry<String, Object> map : data.entrySet()) {
            if (map.getKey().equals("index")) {
                receptor.setIndex((int) map.getValue());
            } else if (row > 1) { // After index and id columns
                SqlVariable variable = receptor.getTable().getLoadedVariable(map.getKey());
                if (variable != null && variable.isLoaded()) {
                    receptor.getActiveContainers().add(new ActiveVariableContainerImpl(variable, receptor, variable.getType().get(map.getValue())));
                } else {
                    receptor.getInactiveContainers().add(new InactiveVariableContainerImpl(map.getKey(), receptor, map.getValue()));
                }
            }
            row++;
        }
    }

    @Override
    public void unload(MysqlReceptor receptor) {
        this.unload(receptor, true);
    }

    @Override
    public boolean isLoaded(MysqlReceptor receptor) {
        return receptor.isLoaded();
    }
}
