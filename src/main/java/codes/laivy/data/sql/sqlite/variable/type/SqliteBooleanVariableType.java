package codes.laivy.data.sql.sqlite.variable.type;

import codes.laivy.data.sql.SqlVariable;
import codes.laivy.data.sql.sqlite.SqliteDatabase;
import codes.laivy.data.sql.sqlite.SqliteVariable;
import codes.laivy.data.sql.sqlite.variable.SqliteVariableType;
import codes.laivy.data.sql.values.SqlParameters;
import codes.laivy.data.sql.values.metadata.SqlMetadata;
import codes.laivy.data.sql.variable.type.SqlBooleanVariableType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLType;

/**
 * As default, the SQLite doesn't have the BOOLEAN data type, then we use the INTEGER data type and set 0 to false, and 1 to true.
 *
 * @author ItsLaivy
 * @since 1.0
 */
public class SqliteBooleanVariableType implements SqlBooleanVariableType<SqliteVariable>, SqliteVariableType<SqliteVariable> {

    private final @NotNull SqliteDatabase database;
    protected @NotNull SQLType type;

    public SqliteBooleanVariableType(@NotNull SqliteDatabase database) {
        this.database = database;

        type = new SQLType() {
            @Override
            public String getName() {
                return "INTEGER";
            }

            @Override
            public String getVendor() {
                return "codes.laivy.data.sql.sqlite.variable.type";
            }

            @Override
            public Integer getVendorTypeNumber() {
                return 1;
            }
        };
    }

    @Override
    public @NotNull SqliteDatabase getDatabase() {
        return database;
    }

    @Override
    public @NotNull SQLType getSqlType() {
        return type;
    }

    @Override
    public void set(@Nullable Object object, @NotNull SqlParameters parameters, @Nullable SqlMetadata metadata) {
        if (object == null) {
            parameters.setNull(getSqlType());
        } else if (object instanceof Boolean) {
            parameters.setBoolean((boolean) object);
        } else {
            throw new IllegalArgumentException("To use the byte variable type, the object needs to be a boolean!");
        }
    }

    @Override
    public @Nullable Boolean get(@Nullable Object object) {
        if (object == null) {
            return null;
        }

        if (object instanceof Boolean) {
            return (boolean) object;
        } else if (object instanceof Integer) {
            return ((int) object) != 0;
        } else {
            throw new IllegalArgumentException("This object doesn't seems to be a boolean object!");
        }
    }

    @Override
    public void configure(@NotNull SqlVariable variable) {
        variable.getDatabase().getManager().getVariablesManager().setType(variable, getSqlType());
    }
}
