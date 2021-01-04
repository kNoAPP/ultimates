package com.knoban.ultimates.cards.impl;

import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.cards.Card;
import com.knoban.ultimates.cards.CardInfo;
import com.knoban.ultimates.primal.PrimalSource;
import com.knoban.ultimates.primal.Tier;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.scheduler.BukkitTask;
import com.knoban.atlas.structure.Pair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@CardInfo(
		material = Material.SAND,
		name = "steady-hands",
		display = "§bSteady Hands", // Typically we want the color to match the Primal
		description = {"§6Sand §7and §6Gravel §7you place", "§7doesn't fall initially."},
		source = PrimalSource.SKY,
		tier = Tier.RARE
)
public class SteadyHandsCard extends Card {
	private final List<Pair<Long, Location>> justPlacedList = new ArrayList<>();
	private final Set<Location> justPlacedSet = new HashSet<>();
	private BukkitTask task;
	private long currentTick;
	
	public SteadyHandsCard(Ultimates plugin) {
		super(plugin);
	}
	
	@Override
	protected void register() {
		super.register();
		currentTick = 0;
		task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
			currentTick++;
			for (Iterator<Pair<Long, Location>> iterator = justPlacedList.iterator(); iterator.hasNext(); ) {
				Pair<Long, Location> pair = iterator.next();
				//noinspection ConstantConditions
				if (pair.getKey() <= currentTick) {
					iterator.remove();
					justPlacedSet.remove(pair.getValue());
				} else {
					break; //We can break here: the elements are in order
				}
			}
		}, 1, 1);
	}
	
	@Override
	protected void unregister() {
		super.unregister();
		task.cancel();
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onBlockPlace(BlockPlaceEvent event) {
		if (shouldAffect(event.getBlockPlaced().getType()) && drawn.contains(event.getPlayer())) {
			//Register that we just placed a sand mid-air and that it shouldn't fall
			//3 ticks is a magic number, anything below that doesn't work
			Location location = event.getBlock().getLocation();
			justPlacedList.add(new Pair<>(currentTick + 3, location));
			justPlacedSet.add(location);
		}
		
		//Placing a non-falling block next to a falling block intentionally makes the falling block fall.
	}
	
	//Lowest: no reason anything should override it, we are just cancelling the event
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onSandFall(EntityChangeBlockEvent event) {
		if (event.getEntityType() == EntityType.FALLING_BLOCK && shouldAffectAndIsJustPlaced(event.getBlock())) {
			//Don't let the just placed block fall
			event.setCancelled(true);
			//This probably notifies the clients not to make the sand fall client-side
			event.getBlock().getState().update(false, false);
		}
	}
	
	//Lowest: no reason anything should override it, we are just cancelling the event
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onBlockUpdate(BlockPhysicsEvent event) {
		if (shouldAffect(event.getBlock().getType())
				|| shouldAffectAndIsJustPlaced(event.getSourceBlock())) {
			//Don't let the just placed block make neighbouring blocks fall
			event.setCancelled(true);
		}
	}
	
	private boolean shouldAffect(Material material) {
		return material == Material.SAND || material == Material.GRAVEL || material == Material.RED_SAND;
	}
	
	private boolean shouldAffectAndIsJustPlaced(Block block) {
		return shouldAffect(block.getType()) && justPlacedSet.contains(block.getLocation());
	}
}
