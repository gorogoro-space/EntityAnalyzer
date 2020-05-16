package space.gorogoro.entityanalyzer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;

/*
 * EntityAnalyzer
 * @license    LGPLv3
 * @copyright  Copyright gorogoro.space 2017
 * @author     kubotan
 * @see        <a href="http://blog.gorogoro.space">Kubotan's blog.</a>
 */
public class EntityAnalyzer extends JavaPlugin {

  public Map<String, Long> checkedMap = new HashMap<>();

  /**
   * JavaPlugin method onEnable.
   */
  @Override
  public void onEnable(){
    // read config.yml
    FileConfiguration config = getConfig();
    try{
      getLogger().info("The Plugin Has Been Enabled!");
      Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
        // public int limitEntity = 100;
    	public int limitEntity = config.getInt("limit_entity");
        public int detectChunkRange = config.getInt("detect_chunk_range");

        @Override
        public void run() {
          for(World w: getServer().getWorlds()) {
            for(Player p:w.getPlayers()) {
              for(Chunk c: p.getWorld().getLoadedChunks()) {
                if(c.getTileEntities().length < limitEntity && c.getEntities().length < limitEntity) {
                  continue;
                }

                if(getDistance(p.getLocation().getChunk().getX(), p.getLocation().getChunk().getZ(), c.getX(), c.getZ()) > detectChunkRange) {
                  continue;
                }

                String checkedKey = c.getX() + "_" + c.getZ();
                if( checkedMap.get(checkedKey) == null || System.currentTimeMillis() - checkedMap.get(checkedKey) > 60 * 60 * 1000d) {
                  String title = "注意";
                  String subtitle = "多数のエンティティーを検知しました。(x:"+(c.getX() * 16)+",z:"+(c.getZ() * 16)+")";
                  p.sendTitle(title, subtitle, 10, 300, 20);
                  checkedMap.put(checkedKey, System.currentTimeMillis());
                  String msg = " " + (detectChunkRange * 16) + "ブロック以内にエンティティーまたはタイルエンティティーが"+limitEntity+"以上のチャンクあります。ホッパー、チェスト、額縁等を分散するか整理をお願いします(x:"+(c.getX() * 16)+",z:"+(c.getZ() * 16)+")";
                  p.sendMessage(ChatColor.DARK_GRAY + msg);
                  getLogger().info(msg + " " + p.getName());
                }
              }
            }
          }

        }
      }, 0L, 100L);

    } catch (Exception e){
      EntityAnalyzerUtility.logStackTrace(e);
    }
  }

  public int getDistance(int x1, int y1, int x2, int y2) {
    return (int) Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
  }

  /**
   * JavaPlugin method onCommand.
   */
  public boolean onCommand( CommandSender sender, Command command, String label, String[] args) {
    try{
      if( command.getName().equals("eatwrank")) {
        Map<String, Integer> rank = new HashMap<>();
        List<World> wlist = this.getServer().getWorlds();
        for(World world: wlist) {
          int cSize = 0;
          for(Chunk c: world.getLoadedChunks()) {
            cSize += c.getTileEntities().length;
          }
          rank.put(world.getName(), cSize);
        }

        // List 生成 (ソート用)
        List<Map.Entry<String,Integer>> entries = getEntrySortedList(rank);

        // 内容を表示
        for (Entry<String,Integer> s : entries) {
          EntityAnalyzerUtility.sendMessage(sender, "WORLD:" + s.getKey() + " ENTITY_COUNT:" + String.valueOf(s.getValue()));
        }
      } else if( command.getName().equals("eawrank")) {

        Map<String, Integer> rank = new HashMap<>();
        List<World> wlist = this.getServer().getWorlds();
        for(World world: wlist) {
          rank.put(world.getName(), world.getEntities().size());
        }

        // List 生成 (ソート用)
        List<Map.Entry<String,Integer>> entries = getEntrySortedList(rank);

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
        List<Map.Entry<String,Integer>> entries = getEntrySortedList(rank);

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
        List<Map.Entry<String,Integer>> entries = getEntrySortedList(rank);
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
        List<Map.Entry<String,Integer>> entries = getEntrySortedList(rank);

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
      else if( command.getName().equals("ealimits")) {
          FileConfiguration config = getConfig();
          String le = config.getString("limit_entity");
          String dcr = config.getString("detect_chunk_range");
          EntityAnalyzerUtility.sendMessage(sender, "Limit Entity: " + le + " Detect Chunk Range: " + dcr);
      }
      else if( command.getName().equals("eanear")) {
          FileConfiguration config = getConfig();
          Integer limit_entity = config.getInt("limit_entity");
          Integer detectChunkRange = config.getInt("detect_chunk_range");
          Player p = (Player) sender;
          for(Chunk c: p.getWorld().getLoadedChunks()) {
	          if(c.getTileEntities().length < limit_entity && c.getEntities().length < limit_entity) {
	            continue;
	          }

	          if(getDistance(p.getLocation().getChunk().getX(), p.getLocation().getChunk().getZ(), c.getX(), c.getZ()) > detectChunkRange) {
	            continue;
	          }

	            String title = "注意";
	            String subtitle = "多数のエンティティーを検知しました。(x:"+(c.getX() * 16)+",z:"+(c.getZ() * 16)+")";
	            p.sendTitle(title, subtitle, 10, 300, 20);
	            String msg = " " + (detectChunkRange * 16) + "ブロック以内にエンティティーまたはタイルエンティティーが"+limit_entity+"以上のチャンクあります。ホッパー、チェスト、額縁等を分散するか整理をお願いします(x:"+(c.getX() * 16)+",z:"+(c.getZ() * 16)+")";
	            p.sendMessage(ChatColor.DARK_GRAY + msg);
	            getLogger().info(msg + " " + p.getName());
	     }
      }
      else if( command.getName().equals("ealimitset")) {
          Player p = (Player) sender;
          if(!p.isOp()) {
        	  EntityAnalyzerUtility.sendMessage(sender, "権限がありません。");
              return false;
          }
          if (args.length != 2){
              return false;
          }
          FileConfiguration config = getConfig();
          String set_target = String.valueOf(args[0]);
          if (!set_target.equals("limit") && !set_target.equals("chunk") ) {
        	  EntityAnalyzerUtility.sendMessage(sender, "First arg is limit or chunk. (" + set_target + ")");
              return false;
          }
          Integer limit = Integer.parseInt(args[1]);
          if (set_target.equals("limit")) {
              Integer upper_limit = config.getInt("lower_limit_entity");
              // 小さすぎる値を排除。過剰反応防止の為
              if (limit < upper_limit) {
            	  EntityAnalyzerUtility.sendMessage(sender, "Second arg over upper limit! limit is : "+ String.valueOf(upper_limit));
                  return false;
              }
              EntityAnalyzerUtility.sendMessage(sender, "Update Limit Entity. -> (" + limit + ")");
              config.set("limit_entity", limit);
          }
          else if (set_target.equals("chunk")) {
        	  Integer upper_limit = config.getInt("upper_limit_detect_chunk_range");
              // 大きすぎる値を排除。過剰探索防止の為
              if (limit > upper_limit) {
            	  EntityAnalyzerUtility.sendMessage(sender,"Second arg over upper limit! limit is : "+ String.valueOf(upper_limit));
                  return false;
              }
              EntityAnalyzerUtility.sendMessage(sender, "Update Detect Chunk Range. -> (" + limit + ")");
              config.set("detect_chunk_range", limit);
          }

          // コンフィグリロード
          saveConfig();
          reloadConfig();
      }
      return true;
    }catch(Exception e){
      EntityAnalyzerUtility.logStackTrace(e);
    }
    return false;
  }

  public List<Map.Entry<String,Integer>> getEntrySortedList(Map<String, Integer> rank){
	  List<Map.Entry<String,Integer>> entries = new ArrayList<Map.Entry<String,Integer>>(rank.entrySet());
      Collections.sort(entries, new Comparator<Map.Entry<String,Integer>>() {
        @Override
        public int compare(
          Entry<String,Integer> entry1, Entry<String,Integer> entry2) {
          return ((Integer)entry2.getValue()).compareTo((Integer)entry1.getValue());
        }
      });
      return entries;
  }

  public double getDistance(double x1, double z1, double x2, double z2) {
    return Math.sqrt(Math.pow((x2 - x1), 2) + Math.pow((z2 - z1), 2));
  }

  /**
   * JavaPlugin method onDisable.
   */
  @Override
  public void onDisable(){
    try{
      getLogger().info("The Plugin Has Been Disabled!");
    } catch (Exception e){
      EntityAnalyzerUtility.logStackTrace(e);
    }
  }
}
