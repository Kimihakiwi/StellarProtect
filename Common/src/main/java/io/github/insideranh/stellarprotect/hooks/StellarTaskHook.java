package io.github.insideranh.stellarprotect.hooks;

import com.cjcrafter.foliascheduler.FoliaCompatibility;
import com.cjcrafter.foliascheduler.ServerImplementation;
import io.github.insideranh.stellarprotect.hooks.tasks.TaskCanceller;
import io.github.insideranh.stellarprotect.StellarProtect;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

public abstract class StellarTaskHook {

    public final Runnable runnable;

    protected StellarTaskHook(Runnable runnable) {
        this.runnable = runnable;
    }

    public abstract TaskCanceller runTask(Location location);

    public abstract TaskCanceller runTask(Location location, long delay);

    public abstract TaskCanceller runTask();

    public abstract TaskCanceller runTask(long delay);

    public abstract TaskCanceller runTaskTimer(long delay, long period);

    public abstract TaskCanceller runTaskTimerAsynchronously(long delay, long period);

    public abstract TaskCanceller runTaskLaterAsynchronously(long delay);


    private static volatile ServerImplementation serverImplementation;

    private static ServerImplementation server() {
        ServerImplementation current = serverImplementation;
        if (current == null) {
            synchronized (StellarTaskHook.class) {
                current = serverImplementation;
                if (current == null) {
                    current = new FoliaCompatibility(StellarProtect.getInstance()).getServerImplementation();
                    serverImplementation = current;
                }
            }
        }
        return current;
    }

    public void runEntity(Entity entity) {
        server().entity(entity).execute(runnable);
    }

    public void runGlobal() {
        server().global().execute(runnable);
    }

}