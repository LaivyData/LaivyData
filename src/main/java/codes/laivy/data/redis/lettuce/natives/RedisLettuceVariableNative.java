package codes.laivy.data.redis.lettuce.natives;

import codes.laivy.data.redis.lettuce.RedisLettuceDatabase;
import codes.laivy.data.redis.lettuce.RedisLettuceVariable;
import codes.laivy.data.redis.variable.RedisVariableType;
import org.intellij.lang.annotations.Pattern;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RedisLettuceVariableNative implements RedisLettuceVariable {

    private final @NotNull RedisLettuceDatabase database;
    private @NotNull @Pattern("^[a-zA-Z_][a-zA-Z0-9_:-]{0,127}$") @Subst("redis_key") String id;
    private final @NotNull RedisVariableType type;
    private final @Nullable Object defValue;

    public RedisLettuceVariableNative(
            @NotNull RedisLettuceDatabase database,
            @NotNull @Pattern("^[a-zA-Z_][a-zA-Z0-9_:-]{0,127}$") @Subst("redis_key") String id,
            @NotNull RedisVariableType type,
            @Nullable Object defValue
    ) {
        this.database = database;
        this.id = id;
        this.type = type;
        this.defValue = defValue;
    }

    private boolean loaded = false;

    @Override
    public @Nullable Object getDefault() {
        return defValue;
    }

    @Override
    @Pattern("^[a-zA-Z_][a-zA-Z0-9_:-]{0,127}$")
    public @NotNull String getId() {
        return id;
    }

    @Override
    public void setId(@NotNull @Pattern("^[a-zA-Z_][a-zA-Z0-9_:-]{0,127}$") @Subst("redis_key") String id) {
        this.id = id;
    }

    @Override
    public void load() {
        getDatabase().getManager().getVariablesManager().load(this);
        loaded = true;
    }

    @Override
    public void unload() {
        getDatabase().getManager().getVariablesManager().unload(this);
        loaded = false;
    }

    @Override
    public void delete() {
        unload();
        getDatabase().getManager().getVariablesManager().delete(this);
    }

    @Override
    public boolean isLoaded() {
        return loaded;
    }

    @Override
    public @NotNull RedisVariableType getType() {
        return type;
    }

    @Override
    public @NotNull RedisLettuceDatabase getDatabase() {
        return database;
    }
}
