package com.cairn4.moonbase.desktop;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.badlogic.gdx.utils.Timer;
import com.cairn4.moonbase.ItemFactory;
import com.cairn4.moonbase.tiles.TileFactory;

import net.rakslice.mewnbase_mod.AltItemFactory;
import net.rakslice.mewnbase_mod.AltTileFactory;
import net.rakslice.mewnbase_mod.FieldPatchHelper;
import net.rakslice.mewnbase_mod.PatchingException;

public class ModDesktopLauncher extends DesktopLauncher {
	
	/** General issues around mod strategy
	 * 
	 * Singletons with private constructors
	 *  - TileFactory
	 */
	
	public ModDesktopLauncher() {
	}
	
	public static void makeSingletonConstructorsAccessible() {
		Constructor<TileFactory> cons;
		try {
			cons = TileFactory.class.getDeclaredConstructor(new Class[] {});
			if (cons == null) {
				throw new PatchingException("Fetching TileFactory constructor returned null");
			}
			cons.setAccessible(true);
		} catch (NoSuchMethodException e) {
			throw new PatchingException("Fetching TileFactory constructor got no such method");
		} catch (SecurityException e) {
			throw new PatchingException("Fetching TileFactory constructor got security exception");
		}
	}
	
	public static void baseLog(String msg) {
		System.out.println("ModDesktopLauncher: " + msg);
	}
	
	public static void log(String msg) {
		baseLog(msg);
	}
	
	public static void installSingletons() {
		log("Installing modified singletons");
		FieldPatchHelper<TileFactory, TileFactory> tileFactoryFieldHelper = new FieldPatchHelper<TileFactory, TileFactory>("ModDesktopLauncher installing TileFactory singleton");
		tileFactoryFieldHelper.setStaticFieldValue(TileFactory.class, "instance", new AltTileFactory());
		
		FieldPatchHelper<ItemFactory, ItemFactory> itemFactoryFieldHelper = new FieldPatchHelper<>("ModDesktopLauncher installing ItemFactory singleton");
		itemFactoryFieldHelper.setStaticFieldValue(ItemFactory.class, "instance", new AltItemFactory());
	}
	
	public static void main(String[] args) {
		log("Starting mod main");
		DesktopLauncher.main(args);
	}

}
