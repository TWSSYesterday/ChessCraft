package me.desht.chesscraft.commands;

import org.bukkit.entity.Player;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.util.ChessUtils;
import me.desht.chesscraft.util.MessageBuffer;

public class PageCommand extends AbstractCommand {

	public PageCommand() {
		super("chess pa", 0, 1);
		setUsage("/chess page");
	}

	@Override
	public boolean execute(ChessCraft plugin, Player player, String[] args) throws ChessException {
		if (args.length < 1) {
			// default is to advance one page and display
			MessageBuffer.nextPage(player);
			MessageBuffer.showPage(player);
		} else if (ChessUtils.partialMatch(args[0], "n")) { //$NON-NLS-1$
			MessageBuffer.nextPage(player);
			MessageBuffer.showPage(player);
		} else if (ChessUtils.partialMatch(args[0], "p")) { //$NON-NLS-1$
			MessageBuffer.prevPage(player);
			MessageBuffer.showPage(player);
		} else {
			try {
				int pageNum = Integer.parseInt(args[0]);
				MessageBuffer.showPage(player, pageNum);
			} catch (NumberFormatException e) {
				ChessUtils.errorMessage(player, Messages.getString("ChessCommandExecutor.invalidNumeric", args[0])); //$NON-NLS-1$
			}
		}
		return true;
	}

}
