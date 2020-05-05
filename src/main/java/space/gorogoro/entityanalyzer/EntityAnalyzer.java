package space.gorogoro.entityanalyzer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

/*
 * EntityAnalyzer
 * @license    LGPLv3
 * @copyright  Copyright gorogoro.space 2017
 * @author     kubotan
 * @see        <a href="http://blog.gorogoro.space">Kubotan's blog.</a>
 */
public class EntityAnalyzer extends JavaPlugin {

  /**
   * JavaPlugin method onEnable.
   */
  @Override
  public void onEnable(){
    try{
      getLogger().log(Level.INFO, "The Plugin Has Been Enabled!");

    } catch (Exception e){
      EntityAnalyzerUtility.logStackTrace(e);
    }
  }

  /**
   * JavaPlugin method onCommand.
   */
  public boolean onCommand( CommandSender sender, Command command, String label, String[] args) {
    try{
      if( command.getName().equals("eawrank")) {

        Map<String, Integer> rank = new HashMap<>();
        List<World> wlist = this.getServer().getWorlds();
        for(World world: wlist) {
          rank.put(world.getName(), world.getEntities().size());
        }

        // List 生成 (ソート用)
        List<Map.Entry<String,Integer>> entries = new ArrayList<Map.Entry<String,Integer>>(rank.entrySet());
        Collections.sort(entries, new Comparator<Map.Entry<String,Integer>>() {
          @Override
          public int compare(
            Entry<String,Integer> entry1, Entry<String,Integer> entry2) {
            return ((Integer)entry2.getValue()).compareTo((Integer)entry1.getValue());
          }
        });

        // 内容を表示
        for (Entry<String,Integer> s : entries) {
          EntityAnalyzerUtility.sendMessage(sender, "WORLD:" + s.getKey() + " ENTITY_COUNT:" + String.valueOf(s.getValue()));
        }

      } else if( command.getName().equals("earank")) {

        if (args.length != 2){
          return false;
        }

        Integer limit = Integer.parseInt(args[1]);
        Map<String, Integer> rank = new HashMap<>();
        World world = getServer().getWorld(args[0]);
        Chunk[] clist = world.getLoadedChunks();
        String key;
        Integer value;
        for(Chunk c: clist) {
          key = String.valueOf(c.getX() * 16) + "," + String.valueOf(c.getZ() * 16);
          value = c.getEntities().length;

          if(rank.get(key) != null){
            value = value + rank.get(key);
          }
          rank.put(key, value);
        }

        // List 生成 (ソート用)
        List<Map.Entry<String,Integer>> entries = new ArrayList<Map.Entry<String,Integer>>(rank.entrySet());
        Collections.sort(entries, new Comparator<Map.Entry<String,Integer>>() {
          @Override
          public int compare(
            Entry<String,Integer> entry1, Entry<String,Integer> entry2) {
            return ((Integer)entry2.getValue()).compareTo((Integer)entry1.getValue());
          }
        });

        // 内容を表示
        EntityAnalyzerUtility.sendMessage(sender, "LOADED CHUNK COUNT:" + clist.length);
        Integer n=0;
        for (Entry<String,Integer> s : entries) {
          n++;
          if(n > limit){
            break;
          }
          EntityAnalyzerUtility.sendMessage(sender, "POSITION:" + s.getKey() + " ENTITY_COUNT:" + String.valueOf(s.getValue()));
        }
      } else if( command.getName().equals("eatrank")) {

        if (args.length != 2){
          return false;
        }

        Integer limit = Integer.parseInt(args[1]);
        Map<String, Integer> rank = new HashMap<>();
        World world = getServer().getWorld(args[0]);
        Chunk[] clist = world.getLoadedChunks();
        String key;
        Integer value;
        for(Chunk c: clist) {
          key = String.valueOf(c.getX() * 16) + "," + String.valueOf(c.getZ() * 16);
          value = c.getTileEntities().length;

          if(rank.get(key) != null){
            value = value + rank.get(key);
          }
          rank.put(key, value);
        }

        // List 生成 (ソート用)
        List<Map.Entry<String,Integer>> entries = new ArrayList<Map.Entry<String,Integer>>(rank.entrySet());
        Collections.sort(entries, new Comparator<Map.Entry<String,Integer>>() {
          @Override
          public int compare(
            Entry<String,Integer> entry1, Entry<String,Integer> entry2) {
            return ((Integer)entry2.getValue()).compareTo((Integer)entry1.getValue());
          }
        });

        // 内容を表示
        EntityAnalyzerUtility.sendMessage(sender, "LOADED CHUNK COUNT:" + clist.length);
        Integer n=0;
        for (Entry<String,Integer> s : entries) {
          n++;
          if(n > limit){
            break;
          }
          EntityAnalyzerUtility.sendMessage(sender, "POSITION:" + s.getKey() + " TILE_ENTITY_COUNT:" + String.valueOf(s.getValue()));
        }
      } else if( command.getName().equals("eathrank")) {

        if (args.length != 2){
          return false;
        }

        Integer limit = Integer.parseInt(args[1]);
        Map<String, Integer> rank = new HashMap<>();
        World world = getServer().getWorld(args[0]);
        Chunk[] clist = world.getLoadedChunks();
        String key;
        Integer value;
        for(Chunk c: clist) {
          key = String.valueOf(c.getX() * 16) + "," + String.valueOf(c.getZ() * 16);
          Integer cntTe=0;
          for(BlockState te:  c.getTileEntities()) {
             if(te.getType() == Material.HOPPER || te.getType() == Material.HOPPER_MINECART) {
               cntTe++;
             }
          }
          value = cntTe;

          if(rank.get(key) != null){
            value = value + rank.get(key);
          }
          rank.put(key, value);
        }

        // List 生成 (ソート用)
        List<Map.Entry<String,Integer>> entries = new ArrayList<Map.Entry<String,Integer>>(rank.entrySet());
        Collections.sort(entries, new Comparator<Map.Entry<String,Integer>>() {
          @Override
          public int compare(
            Entry<String,Integer> entry1, Entry<String,Integer> entry2) {
            return ((Integer)entry2.getValue()).compareTo((Integer)entry1.getValue());
          }
        });

        // 内容を表示
        EntityAnalyzerUtility.sendMessage(sender, "LOADED CHUNK COUNT:" + clist.length);
        Integer n=0;
        for (Entry<String,Integer> s : entries) {
          n++;
          if(n > limit){
            break;
          }
          EntityAnalyzerUtility.sendMessage(sender, "POSITION:" + s.getKey() + " TILE_ENTITY_COUNT:" + String.valueOf(s.getValue()));
        }
      }
      return true;
    }catch(Exception e){
      EntityAnalyzerUtility.logStackTrace(e);
    }
    return false;
  }

  /**
   * JavaPlugin method onDisable.
   */
  @Override
  public void onDisable(){
    try{
      getLogger().log(Level.INFO, "The Plugin Has Been Disabled!");
    } catch (Exception e){
      EntityAnalyzerUtility.logStackTrace(e);
    }
  }
}
