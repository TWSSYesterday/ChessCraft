package me.desht.chesscraft.listeners;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.chess.BoardViewManager;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.inventory.ItemStack;

public class ChessBlockListener extends ChessListenerBase {

	public ChessBlockListener(ChessCraft plugin) {
		super(plugin);
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockDamage(BlockDamageEvent event) {
		if (!plugin.getConfig().getBoolean("no_building", true)) {
			return;
		}
		BoardView bv = BoardViewManager.getManager().partOfChessBoard(event.getBlock().getLocation(), 0);
		if (bv == null || bv.canDesignHere(event.getPlayer(), event.getBlock().getLocation())) {
			return;
		}
		event.setCancelled(true);
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		if (!plugin.getConfig().getBoolean("no_building", true)) {
			return;
		}
		BoardView bv = BoardViewManager.getManager().partOfChessBoard(event.getBlock().getLocation(), 0);
		if (bv == null || bv.canDesignHere(event.getPlayer(), event.getBlock().getLocation())) {
			return;
		}
		event.setCancelled(true);
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent event) {
		if (!plugin.getConfig().getBoolean("no_building", true)) {
			return;
		}
		BoardView bv = BoardViewManager.getManager().partOfChessBoard(event.getBlock().getLocation(), 0);
		if (bv == null || bv.canDesignHere(event.getPlayer(), event.getBlock().getLocation())) {
			return;
		}
		event.setCancelled(true);
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockIgnite(BlockIgniteEvent event) {
		if (!plugin.getConfig().getBoolean("no_burning", true)) {
			return;
		}
		if (BoardViewManager.getManager().partOfChessBoard(event.getBlock().getLocation(), 1) != null) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockBurn(BlockBurnEvent event) {
		if (!plugin.getConfig().getBoolean("no_burning", true)) {
			return;
		}
		if (BoardViewManager.getManager().partOfChessBoard(event.getBlock().getLocation(), 0) != null) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockPhysics(BlockPhysicsEvent event) {
		if (BoardViewManager.getManager().partOfChessBoard(event.getBlock().getLocation(), 0) != null) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockFade(BlockFadeEvent event) {
		if (BoardViewManager.getManager().partOfChessBoard(event.getBlock().getLocation(), 0) != null) {
			event.setCancelled(true);
		}
	}

	/**
	 * Cancelling liquid flow events makes it possible to use water & lava for walls & chess pieces.
	 *
	 * @param event
	 */
	@EventHandler(ignoreCancelled = true)
	public void onBlockFromTo(BlockFromToEvent event) {
		if (BoardViewManager.getManager().partOfChessBoard(event.getBlock().getLocation(), 0) != null) {
			event.setCancelled(true);
		} else if (BoardViewManager.getManager().partOfChessBoard(event.getToBlock().getLocation(), 0) != null) {
			event.setCancelled(true);
		}
	}

	/**
	 * Snow doesn't usually form on chessboards due to the high light level.  But if the light level
	 * is dimmed, we might see boards getting covered.
	 *
	 * @param event
	 */
	@EventHandler(ignoreCancelled = true)
	public void onBlockForm(BlockFormEvent event) {
		if (BoardViewManager.getManager().partOfChessBoard(event.getBlock().getLocation(), 0) != null) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onEntityChangeBlock(EntityChangeBlockEvent event) {
		if (BoardViewManager.getManager().partOfChessBoard(event.getBlock().getLocation(), 0) != null) {
			event.setCancelled(true);
			if (event.getEntity() instanceof FallingBlock) {
				FallingBlock fb = (FallingBlock) event.getEntity();
				if (fb.getDropItem()) {
					fb.getWorld().dropItemNaturally(fb.getLocation(), new ItemStack(fb.getMaterial(), 1, fb.getBlockData()));
				}
			}
		}
	}
}