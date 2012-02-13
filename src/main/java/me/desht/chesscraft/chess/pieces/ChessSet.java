/**
 * Programmer: Jacob Scott
 * Program Name: ChessSet
 * Description: wrapper for all of the chess sets
 * Date: Jul 28, 2011
 */
package me.desht.chesscraft.chess.pieces;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import chesspresso.Chess;
import me.desht.chesscraft.ChessConfig;
import me.desht.chesscraft.ChessPersistence;
import me.desht.chesscraft.blocks.MaterialWithData;
import me.desht.chesscraft.enums.BoardOrientation;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.log.ChessCraftLogger;

public class ChessSet implements Iterable<ChessStone> {

	// map of all known chess sets keyed by set name
	private static final Map<String, ChessSet> allChessSets = new HashMap<String, ChessSet>();

	// map a character to a material
	private MaterialMap materialMapWhite, materialMapBlack;
	
	// map a Chesspresso piece number to a PieceTemplate object
	private final ChessPieceTemplate[] templates = new ChessPieceTemplate[Chess.MAX_PIECE + 1];
	
	// cache of instantiated chess stones
	private final Map<String, ChessStone> stoneCache = new HashMap<String, ChessStone>();
	
	private final String name;
	private int maxWidth = 0;
	private int maxHeight = 0;

	/**
	 * Private constructor.  Initialise a chess set from saved data.
	 * 
	 * @param c		The Configuration object loaded from file.
	 */
	private ChessSet(Configuration c) throws ChessException {
		ChessPersistence.requireSection(c, "name");
		ChessPersistence.requireSection(c, "pieces");
		ChessPersistence.requireSection(c, "materials.white");
		ChessPersistence.requireSection(c, "materials.black");
		
		name = c.getString("name");
		
		ConfigurationSection pieceConf = c.getConfigurationSection("pieces");
		for (String p : pieceConf.getKeys(false)) {
			@SuppressWarnings("unchecked")
			List<List<String>> pieceData = pieceConf.getList(p);
			int piece = Chess.charToPiece(p.charAt(0));
			ChessPieceTemplate tmpl = new ChessPieceTemplate(pieceData);
			if (tmpl.getWidth() > maxWidth) {
				maxWidth = tmpl.getWidth();
			}
			if (tmpl.getSizeY() > maxHeight) {
				maxHeight = tmpl.getSizeY();
			}
			templates[piece] = tmpl;
		}
		
		try {
			materialMapWhite = initMaterialMap(c, "white");
			materialMapBlack = initMaterialMap(c, "black");
		} catch (IllegalArgumentException e) {
			throw new ChessException(e.getMessage());
		}
	}
	
	private MaterialMap initMaterialMap(Configuration c, String colour) {
		MaterialMap res = new MaterialMap();
		ConfigurationSection cs = c.getConfigurationSection("materials." + colour);
		for (String k : cs.getKeys(false)) {
			res.put(k.charAt(0), MaterialWithData.get(cs.getString(k)));
		}
		return res;
	}

	/**
	 * Package protected constructor. Intialise a chess set from template and material map information.  
	 * 
	 * @param name
	 * @param templates
	 * @param materialMapWhite
	 * @param materialMapBlack
	 */
	ChessSet(String name, ChessPieceTemplate[] templates, MaterialMap materialMapWhite, MaterialMap materialMapBlack) {
		this.name = name;
		this.materialMapWhite = materialMapWhite;
		this.materialMapBlack = materialMapBlack;
		for (int piece = Chess.MIN_PIECE + 1; piece <= Chess.MAX_PIECE; piece++) {
			this.templates[piece] = templates[piece];
		}
	}

	public Iterator<ChessStone> iterator() {
		return new ChessPieceIterator();
	}

	/**
	 * Return a mapping of white material to black material for those materials in this set
	 * which differ for the white and black pieces.
	 * 
	 * @return
	 */
	public Map<String, String> getWhiteToBlack() {
		Map<String,String> res = new HashMap<String, String>();
		
		for (Entry<Character,MaterialWithData> e : materialMapWhite.getMap().entrySet()) {
			String w = e.getValue().toString();
			String b = materialMapBlack.get(e.getKey()).toString();
			if (!w.equals(b)) {
				res.put(w, b);
			}
		}
		
		return res;
	}
	
	/**
	 * Retrieve a fully instantied chess stone.  It will use material appropriate for the player's 
	 * colour, and will be rotated in the right direction given the player and board orienation.
	 * 
	 * @param piece		Chesspresso piece number (Chess.PAWN, Chess.KNIGHT etc.)
	 * @param colour	Chesspresso colour (Chess.WHITE or Chess.BLACK)
	 * @param direction		Board orientation
	 * @return
	 */
	public ChessStone getStone(int piece, int colour, BoardOrientation direction) {
		String key = String.format("%d:%d:%s", piece, colour, direction);
		if (!stoneCache.containsKey(key)) {
			MaterialMap matMap = colour == Chess.WHITE ? materialMapWhite : materialMapBlack;
			int stone = Chess.pieceToStone(piece, colour);
			stoneCache.put(key, new ChessStone(stone, templates[piece], matMap, direction));
		}
		return stoneCache.get(key);
	}
	
	/**
	 * Retrieve a fully instantied chess stone.
	 * 
	 * @param stone		Chesspresso stone number (Chess.WHITE_PAWN etc.)
	 * @param direction		Board orientation
	 * @return
	 */
	public ChessStone getStone(int stone, BoardOrientation direction) {
		int piece = Chess.stoneToPiece(stone);
		int colour = Chess.stoneToColor(stone);
		String key = String.format("%d:%d:%s", piece, colour, direction);
		if (!stoneCache.containsKey(key)) {
			MaterialMap matMap = colour == Chess.WHITE ? materialMapWhite : materialMapBlack;
			stoneCache.put(key, new ChessStone(stone, templates[piece], matMap, direction));
		}
		return stoneCache.get(key);
	}

	/**
	 * Get this chess set's name.
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Return the width (X or Z) of the widest piece in the set
	 *
	 * @return
	 */
	public int getMaxWidth() {
		return maxWidth;
	}

	/**
	 * Return the height of the tallest piece in the set
	 * 
	 * @return
	 */
	public int getMaxHeight() {
		return maxHeight;
	}

	/**
	 * Save this chess set to a file with the new name.
	 * 
	 * @param newName
	 * @throws ChessException
	 */
	public void save(String newName) throws ChessException {
		File f = ChessConfig.getResourceFile(ChessConfig.getPieceStyleDirectory(), ChessPersistence.makeSafeFileName(newName), true);
		
		YamlConfiguration conf = new YamlConfiguration();
		try {
			conf.set("name", name);
			for (char c : materialMapWhite.getMap().keySet()) {
				conf.set("materials.white." + c, materialMapWhite.get(c).toString());
			}
			for (char c : materialMapBlack.getMap().keySet()) {
				conf.set("materials.black." + c, materialMapBlack.get(c).toString());
			}
			for (int piece = Chess.MIN_PIECE + 1; piece <= Chess.MAX_PIECE; piece++) {
				conf.set("pieces." + Chess.pieceToChar(piece), templates[piece].getPieceData());
			}
			conf.save(f);
		} catch (IOException e) {
			throw new ChessException(e.getMessage());
		}
	}
	
	//--------------------------static methods---------------------------------
	
	public static boolean loaded(String setName) {
		return allChessSets.containsKey(setName);
	}
	
	/**
	 * Retrieve a chess set with the given name, loading it from file if necessary.
	 * 
	 * @param setName
	 * @return
	 * @throws ChessException
	 */
	public static ChessSet getChessSet(String setName) throws ChessException {
		if (!loaded(setName)) {
			allChessSets.put(setName, loadChessSet(setName));
		}
		return allChessSets.get(setName);
	}

	public static String[] getChessSetNames() {
		return allChessSets.keySet().toArray(new String[0]);
	}

	public static ChessSet[] getAllChessSets() {
		return allChessSets.values().toArray(new ChessSet[0]);
	}
	
	private static ChessSet loadChessSet(String setFileName) throws ChessException {
		File f = ChessConfig.getResourceFile(ChessConfig.getPieceStyleDirectory(), setFileName);
		
		Configuration c = YamlConfiguration.loadConfiguration(f);
		ChessSet set = new ChessSet(c);
		ChessCraftLogger.log("loaded set " + set.getName() + " OK.");
		
		return set;
	}

	
	//-------------------------------- iterator class
	
	public class ChessPieceIterator implements Iterator<ChessStone> {

		int i = 0;
		Integer keys[] = new Integer[0];
		
		public ChessPieceIterator(){
			keys = stoneCache.keySet().toArray(keys);
		}
		
		public boolean hasNext() {
			return keys.length > i;
		}

		public ChessStone next() {
			// simply iterates through values.. not through keys
			return stoneCache.get(keys[i++]);
		}

		public void remove() {
		}

	}
} // end class ChessSet

