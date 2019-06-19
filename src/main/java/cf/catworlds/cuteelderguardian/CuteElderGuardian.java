package cf.catworlds.cuteelderguardian;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import org.bukkit.entity.ElderGuardian;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CuteElderGuardian extends JavaPlugin implements Listener {

    private static Set<UUID> uuidFromData;
    private static Set<ElderGuardian> elderGuardianPets = new HashSet<>();
    private static final String DATA_FILE_NAME = "ElderGuardianPet.temp";

    public static void addPet(ElderGuardian pet) {
        addPetFix(pet);
    }

    @Override
    public void onEnable() {
        uuidFromData = loadData();
        getServer().getPluginManager().registerEvents(this, this);
        startFixTimer();
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
        if (!saveData()) {
            getLogger().warning("Save Data fail");
        }
    }

    private void startFixTimer() {
        getServer().getScheduler().scheduleSyncRepeatingTask(this,
                () -> elderGuardianPets.forEach(CuteElderGuardian::resetPetClock),
                100, 1198);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPetLoad(EntityAddToWorldEvent event) {
        if (event.getEntityType() != EntityType.ELDER_GUARDIAN)
            return;
        if (uuidFromData.contains(event.getEntity().getUniqueId()))
            addPetFix((ElderGuardian) event.getEntity());
    }

    @EventHandler
    public void onPluginSpawnPet(CreatureSpawnEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof ElderGuardian))
            return;

        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.CUSTOM)
            return;

        if (!entity.getWorld().getName().contains("world"))
            return;

        resetPetClock((ElderGuardian) entity);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPetDead(EntityDeathEvent event) {
        Entity elderGuardian = event.getEntity();
        if (!(elderGuardian instanceof ElderGuardian))
            return;
        elderGuardianPets.remove(event.getEntity());
        uuidFromData.remove(event.getEntity().getUniqueId());
    }

    private static void addPetFix(ElderGuardian pet) {
        resetPetClock(pet);
        elderGuardianPets.add(pet);
    }

    private static void resetPetClock(ElderGuardian pet) {
        // cheat for nms_1.12.2
        // when (id+liveTick) %1200 == 0 then apply debuff to player
        pet.setTicksLived(1201 - (pet.getEntityId() % 1200));
    }

    private HashSet<UUID> loadData() {
        File dataFile = new File(getDataFolder(), DATA_FILE_NAME);
        if (dataFile.exists()) {
            FileInputStream fis;
            try {
                fis = new FileInputStream(dataFile);
                ObjectInputStream ois = new ObjectInputStream(fis);
                @SuppressWarnings("unchecked")
                HashSet<UUID> read = (HashSet<UUID>) ois.readObject();
                ois.close();
                return read;
            } catch (Exception ignore) {
            }
        }
        return new HashSet<>();
    }

    /**
     * @return true if save to file success
     */
    private boolean saveData() {
        File dataFile = new File(getDataFolder(), DATA_FILE_NAME);
        if (!dataFile.getParentFile().exists()) {
            if (dataFile.getParentFile().mkdirs())
                return false;
        }
        try {
            FileOutputStream fos = new FileOutputStream(dataFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            for (ElderGuardian pet : elderGuardianPets)
                uuidFromData.add(pet.getUniqueId());
            oos.writeObject(uuidFromData);
            oos.flush();
            oos.close();
            return true;
        } catch (Exception ignore) {
        }
        return false;
    }

}
