package io.github.insideranh.stellarprotect.nms.v1_21_R11.listeners;

import io.github.insideranh.stellarprotect.api.events.EventLogicHandler;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.EntityMountEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.event.player.PlayerUnleashEntityEvent;
import org.bukkit.event.raid.RaidFinishEvent;
import org.bukkit.event.raid.RaidSpawnWaveEvent;
import org.bukkit.event.raid.RaidStopEvent;
import org.bukkit.event.raid.RaidTriggerEvent;
import org.bukkit.event.world.PortalCreateEvent;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class BlockListener_v1_21_R11 implements Listener {

    private final EventLogicHandler eventLogicHandler;

    public BlockListener_v1_21_R11(EventLogicHandler eventLogicHandler) {
        this.eventLogicHandler = eventLogicHandler;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPortalCreate(PortalCreateEvent event) {
        this.eventLogicHandler.onPortalCreate(event.getBlocks().stream().map(BlockState::getBlock).collect(Collectors.toCollection(ArrayList::new)));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSmithEvent(SmithItemEvent event) {
        this.eventLogicHandler.onSmithEvent(event.getWhoClicked(), event.getCurrentItem());
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTotemEvent(EntityResurrectEvent event) {
        this.eventLogicHandler.onTotemEvent(event.getEntity(), event.getHand() == null ? "" : event.getHand().name());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRaid(RaidTriggerEvent event) {
        this.eventLogicHandler.onRaidTrigger(event.getPlayer(), event.getRaid());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRaid(RaidStopEvent event) {
        if (event.getReason() != RaidStopEvent.Reason.FINISHED) {
            this.eventLogicHandler.onRaidFinish(event.getRaid());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRaid(RaidSpawnWaveEvent event) {
        this.eventLogicHandler.onRaidSpawn(event.getRaid());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRaid(RaidFinishEvent event) {
        this.eventLogicHandler.onRaidFinish(event.getRaid());
    }

    @EventHandler
    public void onMount(EntityMountEvent event) {
        this.eventLogicHandler.onMount(event.getEntity(), event.getMount());
    }

    @EventHandler
    public void onDismount(EntityDismountEvent event) {
        this.eventLogicHandler.onDismount(event.getEntity(), event.getDismounted());
    }

    @EventHandler
    public void onLeash(PlayerLeashEntityEvent event) {
        this.eventLogicHandler.onLeash(event.getPlayer(), event.getEntity());
    }

    @EventHandler
    public void onUnleash(PlayerUnleashEntityEvent event) {
        this.eventLogicHandler.onUnleash(event.getPlayer(), event.getEntity());
    }

}