package me.desht.chesscraft.blocks;

import java.util.HashMap;
import java.util.Map;

import me.desht.dhutils.LogUtils;
import me.desht.chesscraft.regions.Cuboid;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;

import com.google.common.base.Joiner;

public class MaterialWithData implements Cloneable {

	private final static Map<String, MaterialWithData> materialCache = new HashMap<String, MaterialWithData>();

	final int material;
	final byte data;
	final String[] metadata; // e.g. sign text

	private MaterialWithData(int mat, byte d) {
		material = mat;
		data = d;
		metadata = null;
	}

	private MaterialWithData(int mat) {
		this(mat, (byte) 0);
	}

	private MaterialWithData(MaterialWithData m) {
		material = m.material;
		data = m.data;
		metadata = m.metadata;
	}

	private MaterialWithData(String string) {
		String[] matAndText = string.split("=");
		String[] matAndData = matAndText[0].split(":");

		LogUtils.finest("MaterialWithData constructor: " + string);
		metadata = matAndText.length > 1 ? makeText(matAndText[1]) : null;

		if (matAndData[0].matches("^[0-9]+$")) {
			material = Integer.parseInt(matAndData[0]);
		} else {
			// we'll look for the material string first in the WorldEdit BlockType class
			// and if that fails, we'll check for a Bukkit Material,
			// and if that fails, just throw an IllegalArgumentException
			BlockType b = BlockType.lookup(matAndData[0], true);
			if (b == null) {
				Material m = Material.matchMaterial(matAndData[0]);
				if (m == null) {
					throw new IllegalArgumentException("unknown material: " + matAndData[0]);
				}
				material = m.getId();
			} else {
				material = b.getID();
			}
		}
		if (matAndData.length < 2) {
			data = 0;
		} else {
			if (matAndData[1].matches("^[0-9]+$")) {
				data = Byte.parseByte(matAndData[1]);
			} else if (material == BlockType.CLOTH.getID()) {
				ClothColor cc = ClothColor.lookup(matAndData[1]);
				if (cc == null) {
					throw new IllegalArgumentException("unknown dye colour: " + matAndData[1]);
				}
				data = (byte) cc.getID();
			} else {
				throw new IllegalArgumentException("invalid data specification: " + matAndData[1]);
			}
		}
	}

	/**
	 * Get the data byte for this MaterialWithData object
	 * 
	 * @return the material data byte
	 */
	public Byte getData() {
		return data;
	}

	/**
	 * Get the material ID for this MaterialWithData object
	 * 
	 * @return the material ID
	 */
	public int getMaterialId() {
		return material;
	}

	/**
	 * Get the extra data for this MaterialWithData object
	 * 
	 * @return list of Strings representing extra data for this object
	 */
	public String[] getText() {
		return metadata;
	}

	/**
	 * Get a MaterialData object from a String specification. The specification is a string or numeric Material name,
	 * optionally followed by a colon (:) and a numeric data byte.
	 * 
	 * @param spec
	 *            The specification
	 * @return The MaterialWithData object
	 * @throws IllegalArgumentException
	 *             if the specification is invalid
	 */
	public static MaterialWithData get(String spec) {
		spec = spec.toLowerCase();
		if (!materialCache.containsKey(spec)) {
			MaterialWithData mat = new MaterialWithData(spec);
			materialCache.put(spec, mat);
		}
		return materialCache.get(spec);
	}

	/**
	 * Get a MaterialData from a numeric ID and data byte, and extra material-dependent metadata (e.g. the text on a
	 * sign).
	 * 
	 * @param id
	 *            material ID
	 * @param data
	 *            material data byte
	 * @param metadata
	 *            list of Strings representing extra data for this object
	 * @return The MaterialWithData object
	 */
	public static MaterialWithData get(int id, byte data, String[] metadata) {
		String key = metadata == null ? String.format("%d:%d", id, data) : String.format("%d:%d=%s", id, data, Joiner
				.on(";").join(metadata));
		return get(key);
	}

	/**
	 * Get a MaterialData from a numeric ID and data byte.
	 * 
	 * @param id
	 *            the material ID
	 * @param data
	 *            the material data byte
	 * @return The MaterialWithData object
	 */
	public static MaterialWithData get(int id, byte data) {
		return get(String.format("%d:%d", id, data));
	}

	/**
	 * Get a MaterialData from a numeric ID. The data byte will be 0.
	 * 
	 * @param id
	 *            the material ID
	 * @return The MaterialWithData object
	 */
	public static MaterialWithData get(int id) {
		return get(String.format("%d:%d", id, 0));
	}

	private String[] makeText(String input) {
		String[] t = new String[] { "", "", "", "" };
		String[] s = input.split(";");
		for (int i = 0; i < 4 && i < s.length; i++) {
			t[i] = s[i];
		}
		return t;
	}

	/**
	 * Apply this MaterialWithData to the given block.
	 * 
	 * @param b
	 *            The block to apply the material to
	 */
	public void applyToBlock(Block b) {
		b.setTypeIdAndData(material, data, false);
		if (metadata != null && (material == 63 || material == 68)) {
			// updating a wall sign or floor sign, with text
			Sign sign = (Sign) b.getState().getData();
			for (int i = 0; i < 4; i++) {
				sign.setLine(i, metadata[i]);
			}
			sign.update();
		}
	}

	/**
	 * Use direct NMS calls to apply this MaterialWithData to a block without the overhead of lighting recalculation
	 * etc. Use this during mass block updates. The caller is responsible for subsequently ensuring that lighting is
	 * re-initialised and clients are notified of any changes.
	 * 
	 * @param b
	 *            The block to apply the material to
	 */
	public void applyToBlockFast(Block b) {
		BlockUtils.setBlockFast(b, material, data);
		if (metadata != null && (material == 63 || material == 68)) {
			// updating a wall sign or floor sign, with text
			Sign sign = (Sign) b.getState().getData();
			for (int i = 0; i < 4; i++) {
				sign.setLine(i, metadata[i]);
			}
			sign.update();
		}
	}

	/**
	 * Apply this MaterialWithData to all the blocks within the given Cuboid using fast NMS calls. The caller is
	 * responsible for subsequently ensuring that lighting is re-initialised and clients are notified of any changes.
	 * 
	 * @param c
	 */
	public void applyToCuboid(Cuboid c) {
		if (c != null) {
			c.setFast(material, data);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder s = new StringBuilder(Material.getMaterial(material).toString());
		if (material == 35) { // wool
			s.append(":").append(DyeColor.getByData(data).toString());
		} else {
			s.append(":").append(Byte.toString(data));
		}
		return s.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#clone()
	 */
	@Override
	public MaterialWithData clone() {
		return new MaterialWithData(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + data;
		result = prime * result + material;
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		MaterialWithData other = (MaterialWithData) obj;
		if (material != other.material) {
			return false;
		} else if (data != other.data) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Get a rotated version of this MaterialData by altering the data byte appropriately.
	 * 
	 * @param rotation
	 *            The rotation in degrees; must be one of 90, 180 or 270 (any other value will return the original
	 *            material unchanged)
	 * @return the rotated material
	 */
	public MaterialWithData rotate(int rotation) {
		byte newData = data;
		switch (rotation) {
		case 270:
			newData = (byte) BlockData.rotate90Reverse(material, data);
			break;
		case 180:
			newData = (byte) BlockData.rotate90(material, data);
			newData = (byte) BlockData.rotate90(material, newData);
			break;
		case 90:
			newData = (byte) BlockData.rotate90(material, data);
			break;
		}
		return MaterialWithData.get(material, newData, metadata);
	}

}
