package space.gorogoro.entityanalyzer;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.util.Ticks;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
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

  // 実行中の集計(サーバー全体で 1 つまで)
  private ChunkScanTask runningScan = null;
  // 集計を実行した人ごとの、次に実行できる時刻
  private final Map<String, Long> cooldownMap = new HashMap<>();

  /**
   * JavaPlugin method onEnable.
   */
  @Override
  public void onEnable() {
    try {
      getLogger().info("The Plugin Has Been Enabled!");

      // If there is no setting file, it is created
      if(!getDataFolder().exists()){
        getDataFolder().mkdir();
      }
      File configFile = new File(getDataFolder() + "/config.yml");
      if(!configFile.exists()){
        saveDefaultConfig();
      }

      Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
        @Override
        public void run() {
          if(!getConfig().getBoolean("notice")) {
            return;
          }
          // 1 時間を過ぎた記録は消す
          long now = System.currentTimeMillis();
          checkedMap.values().removeIf(t -> now - t > 60 * 60 * 1000L);
          for (World w : getServer().getWorlds()) {
            for (Player p : w.getPlayers()) {
              for (Chunk c : getNearbyCrowdedChunks(p, getConfig().getInt("limit_entity"),
                  getConfig().getInt("detect_chunk_range"))) {
                String checkedKey = c.getWorld().getName() + "_" + c.getX() + "_" + c.getZ();
                if (checkedMap.get(checkedKey) == null) {
                  String title = "注意";
                  String subtitle = "多数のエンティティーを検知しました。(x:" + (c.getX() * 16) + ",z:"
                      + (c.getZ() * 16) + ")";
                  p.showTitle(Title.title(Component.text(title), Component.text(subtitle),
                      Title.Times.times(Ticks.duration(10), Ticks.duration(300), Ticks.duration(20))));
                  checkedMap.put(checkedKey, System.currentTimeMillis());
                  String msg = " " + (getConfig().getInt("detect_chunk_range") * 16) + "ブロック以内にエンティティーまたはタイルエンティティーが"
                      + getConfig().getInt("limit_entity") + "以上のチャンクあります。ホッパー、チェスト、額縁等を分散するか整理をお願いします(x:"
                      + (c.getX() * 16) + ",z:" + (c.getZ() * 16) + ")";
                  p.sendMessage(Component.text(msg, NamedTextColor.DARK_GRAY));
                  getLogger().info(msg + " " + p.getName());
                }
              }
            }
          }
        }
      }, 0L, 20L);
    } catch (Exception e) {
      EntityAnalyzerUtility.logStackTrace(e);
    }
  }

  /**
   * JavaPlugin method onCommand.
   */
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    try {
      if (command.getName().equals("eastatus")) {
        if (!sender.isOp()) {
          EntityAnalyzerUtility.sendMessage(sender, "権限がありません。");
          return false;
        }

        if (args.length < 2) {
          List<World> wlist = new ArrayList<World>();
          if(args.length == 1) {
            World world = findWorld(sender, args[0]);
            if (world == null) {
              return true;
            }
            wlist.add(world);
          }else {
            wlist.addAll(getServer().getWorlds());
          }
          if (!canStartScan(sender)) {
            return true;
          }
          Map<String, Map<String, Integer>> eranks = new LinkedHashMap<>();
          Map<String, Map<String, Integer>> tranks = new LinkedHashMap<>();
          List<Chunk> chunks = new ArrayList<>();
          for (World world : wlist) {
            eranks.put(world.getName(), new HashMap<>());
            tranks.put(world.getName(), new HashMap<>());
            chunks.addAll(Arrays.asList(world.getLoadedChunks()));
          }

          startScan(sender, chunks, c -> {
            // エンティティー種類ごとのカウント
            Map<String, Integer> erank = eranks.get(c.getWorld().getName());
            for (Entity e: c.getEntities()) {
              erank.merge(e.getType().toString(), 1, Integer::sum);
            }

            // タイルエンティティー種類ごとのカウント
            Map<String, Integer> trank = tranks.get(c.getWorld().getName());
            for (BlockState t: c.getTileEntities(false)) {
              trank.merge(t.getType().toString(), 1, Integer::sum);
            }
          }, () -> {
            for (World world : wlist) {
              int printNum = 0;
              // エンティティー結果表示
              String msg = "========== Entity Type Rank WORLD:" + world.getName() + " ==========";
              EntityAnalyzerUtility.sendMessage(sender, msg);
              getServer().getLogger().info(msg);
              for (Entry<String, Integer> s : getEntrySortedList(eranks.get(world.getName()))) {
                printNum++;
                String emsg = "  ENTITY:" + s.getKey() + " COUNT:" + String.valueOf(s.getValue());
                EntityAnalyzerUtility.sendMessage(sender, emsg);
                getServer().getLogger().info(emsg);
                if(printNum >= 10) {
                  break;
                }
              }

              printNum = 0;
              // タイルエンティティー結果表示
              msg = "========== TileEntity Type Rank WORLD:" + world.getName() + " ==========";
              EntityAnalyzerUtility.sendMessage(sender, msg);
              getServer().getLogger().info(msg);
              for (Entry<String, Integer> s : getEntrySortedList(tranks.get(world.getName()))) {
                printNum++;
                String tmsg = "  TILE_ENTITY:" + s.getKey() + " COUNT:" + String.valueOf(s.getValue());
                EntityAnalyzerUtility.sendMessage(sender, tmsg);
                getServer().getLogger().info(tmsg);
                if(printNum >= 10) {
                  break;
                }
              }

              EntityAnalyzerUtility.sendMessage(sender, "========================================================================");
              getServer().getLogger().info("========================================================================");
            }
          });
        } else {
          World world = findWorld(sender, args[0]);
          if (world == null) {
            return true;
          }
          String target = args[1];
          if (!canStartScan(sender)) {
            return true;
          }

          Map<String, Integer> erank = new HashMap<>();
          Map<String, Integer> trank = new HashMap<>();

          startScan(sender, Arrays.asList(world.getLoadedChunks()), c -> {
            String key = String.valueOf(c.getX() * 16) + "," + String.valueOf(c.getZ() * 16);

            int ecnt = 0;
            for(Entity e: c.getEntities()) {
              if (e.getType().toString().equals(target)) {
                ecnt++;
              }
            }
            erank.merge(key, ecnt, Integer::sum);
            int tcnt = 0;
            for(BlockState t: c.getTileEntities(false)) {
              if (t.getType().toString().equals(target)) {
                tcnt++;
              }
            }
            trank.merge(key, tcnt, Integer::sum);
          }, () -> {
            int printNum = 0;
            String msg = "========== Entity Type Rank TYPE:" + target + " ==========";
            EntityAnalyzerUtility.sendMessage(sender, msg);
            List<Map.Entry<String, Integer>> eresult = getEntrySortedList(erank);
            int esum = 0;
            for (Entry<String, Integer> s : eresult) {
              if(s.getValue() <= 0) {
                continue;
              }
              msg = "  POSITION:" + s.getKey() + " ENTITY_COUNT:" + String.valueOf(s.getValue());
              getServer().getLogger().info(msg);
              EntityAnalyzerUtility.sendMessage(sender, msg);
              esum = esum + s.getValue();
              printNum++;
              if(printNum >= 10) {
                break;
              }
            }

            printNum = 0;
            List<Map.Entry<String, Integer>> tresult = getEntrySortedList(trank);
            int tsum = 0;
            for (Entry<String, Integer> s : tresult) {
              if(s.getValue() <= 0) {
                continue;
              }
              msg = "  POSITION:" + s.getKey() + " TILE_ENTITY_COUNT:" + String.valueOf(s.getValue());
              getServer().getLogger().info(msg);
              EntityAnalyzerUtility.sendMessage(sender, msg);
              tsum = tsum + s.getValue();
              printNum++;
              if(printNum >= 10) {
                break;
              }
            }
            msg = "esum:" + String.valueOf(esum) + " tsum:" + String.valueOf(tsum);
            getServer().getLogger().info(msg);
            EntityAnalyzerUtility.sendMessage(sender, msg);
          });
        }
      } else if (command.getName().equals("eatwrank")) {
        Map<String, Integer> rank = new HashMap<>();
        List<World> wlist = this.getServer().getWorlds();
        for (World world : wlist) {
          rank.put(world.getName(), world.getTileEntityCount());
        }
        // List 生成 (ソート用)
        List<Map.Entry<String, Integer>> entries = getEntrySortedList(rank);
        // 内容を表示
        for (Entry<String, Integer> s : entries) {
          EntityAnalyzerUtility.sendMessage(sender,
              "WORLD:" + s.getKey() + " ENTITY_COUNT:" + String.valueOf(s.getValue()));
        }
      } else if (command.getName().equals("eawrank")) {
        Map<String, Integer> rank = new HashMap<>();
        List<World> wlist = this.getServer().getWorlds();
        for (World world : wlist) {
          rank.put(world.getName(), world.getEntityCount());
        }
        // List 生成 (ソート用)
        List<Map.Entry<String, Integer>> entries = getEntrySortedList(rank);
        // 内容を表示
        for (Entry<String, Integer> s : entries) {
          EntityAnalyzerUtility.sendMessage(sender,
              "WORLD:" + s.getKey() + " ENTITY_COUNT:" + String.valueOf(s.getValue()));
        }

      } else if (command.getName().equals("earank")) {
        return chunkRank(sender, args, "ENTITY_COUNT", c -> c.getEntities().length);
      } else if (command.getName().equals("eatrank")) {
        return chunkRank(sender, args, "TILE_ENTITY_COUNT", c -> c.getTileEntities(false).length);
      } else if (command.getName().equals("eathrank")) {
        return chunkRank(sender, args, "TILE_ENTITY_COUNT", c -> {
          int cntTe = 0;
          for (BlockState te : c.getTileEntities(false)) {
            if (te.getType() == Material.HOPPER || te.getType() == Material.HOPPER_MINECART) {
              cntTe++;
            }
          }
          return cntTe;
        });
      } else if (command.getName().equals("ealimits")) {
        FileConfiguration config = getConfig();
        String le = config.getString("limit_entity");
        String dcr = config.getString("detect_chunk_range");
        EntityAnalyzerUtility.sendMessage(sender, "Limit Entity: " + le + " Detect Chunk Range: " + dcr);
      } else if (command.getName().equals("eanear")) {
        if (!sender.isOp()) {
          EntityAnalyzerUtility.sendMessage(sender, "権限がありません。");
          return false;
        }
        if (!(sender instanceof Player)) {
          EntityAnalyzerUtility.sendMessage(sender, "Console can't this command.");
          return false;
        }
        FileConfiguration config = getConfig();
        Integer limit_entity = config.getInt("limit_entity");
        Integer detectChunkRange = config.getInt("detect_chunk_range");
        Player p = (Player) sender;
        for (Chunk c : getNearbyCrowdedChunks(p, limit_entity, detectChunkRange)) {
          String title = "注意";
          String subtitle = "多数のエンティティーを検知しました。(x:" + (c.getX() * 16) + ",z:" + (c.getZ() * 16) + ")";
          p.showTitle(Title.title(Component.text(title), Component.text(subtitle),
              Title.Times.times(Ticks.duration(10), Ticks.duration(300), Ticks.duration(20))));
          String msg = " " + (detectChunkRange * 16) + "ブロック以内にエンティティーまたはタイルエンティティーが" + limit_entity
              + "以上のチャンクあります。ホッパー、チェスト、額縁等を分散するか整理をお願いします(x:" + (c.getX() * 16) + ",z:" + (c.getZ() * 16)
              + ")";
          p.sendMessage(Component.text(msg, NamedTextColor.DARK_GRAY));
          getLogger().info(msg + " " + p.getName());
        }
      } else if (command.getName().equals("eareload")) {
        if (!sender.isOp()) {
          EntityAnalyzerUtility.sendMessage(sender, "権限がありません。");
          return false;
        }
        reloadConfig();
        EntityAnalyzerUtility.sendMessage(sender, "config.yml を読み直しました。");
      } else if (command.getName().equals("ealimitset")) {
        if (!sender.isOp()) {
          EntityAnalyzerUtility.sendMessage(sender, "権限がありません。");
          return false;
        }
        if (args.length != 2) {
          return false;
        }
        FileConfiguration config = getConfig();
        String set_target = String.valueOf(args[0]);
        if (!set_target.equals("limit") && !set_target.equals("chunk")) {
          EntityAnalyzerUtility.sendMessage(sender, "First arg is limit or chunk. (" + set_target + ")");
          return false;
        }
        Integer limit = parseNumber(sender, args[1]);
        if (limit == null) {
          return true;
        }
        if (set_target.equals("limit")) {
          Integer upper_limit = config.getInt("lower_limit_entity");
          // 小さすぎる値を排除。過剰反応防止の為
          if (limit < upper_limit) {
            EntityAnalyzerUtility.sendMessage(sender,
                "Second arg over upper limit! limit is : " + String.valueOf(upper_limit));
            return false;
          }
          EntityAnalyzerUtility.sendMessage(sender, "Update Limit Entity. -> (" + limit + ")");
          config.set("limit_entity", limit);
        } else if (set_target.equals("chunk")) {
          Integer upper_limit = config.getInt("upper_limit_detect_chunk_range");
          // 大きすぎる値を排除。過剰探索防止の為
          if (limit > upper_limit) {
            EntityAnalyzerUtility.sendMessage(sender,
                "Second arg over upper limit! limit is : " + String.valueOf(upper_limit));
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
    } catch (Exception e) {
      EntityAnalyzerUtility.logStackTrace(e);
    }
    return false;
  }

  /**
   * /earank・/eatrank・/eathrank の共通処理。チャンクごとの数を多い順に limit 件表示する。
   */
  private boolean chunkRank(CommandSender sender, String[] args, String label,
      ToIntFunction<Chunk> countFunc) {
    if (args.length != 2) {
      return false;
    }
    World world = findWorld(sender, args[0]);
    if (world == null) {
      return true;
    }
    Integer limit = parseNumber(sender, args[1]);
    if (limit == null) {
      return true;
    }
    if (!canStartScan(sender)) {
      return true;
    }
    Map<String, Integer> rank = new HashMap<>();
    Chunk[] clist = world.getLoadedChunks();
    startScan(sender, Arrays.asList(clist), c -> {
      String key = String.valueOf(c.getX() * 16) + "," + String.valueOf(c.getZ() * 16);
      rank.merge(key, countFunc.applyAsInt(c), Integer::sum);
    }, () -> {
      // List 生成 (ソート用)
      List<Map.Entry<String, Integer>> entries = getEntrySortedList(rank);
      // 内容を表示
      EntityAnalyzerUtility.sendMessage(sender, "LOADED CHUNK COUNT:" + clist.length);
      Integer n = 0;
      for (Entry<String, Integer> s : entries) {
        n++;
        if (n > limit) {
          break;
        }
        EntityAnalyzerUtility.sendMessage(sender,
            "POSITION:" + s.getKey() + " " + label + ":" + String.valueOf(s.getValue()));
      }
    });
    return true;
  }

  /**
   * プレイヤーの周り range チャンク以内で、エンティティーかタイルエンティティーが limitEntity 以上のチャンクを返す。
   * 読み込まれていないチャンクは読み込まずに飛ばす。
   */
  private List<Chunk> getNearbyCrowdedChunks(Player p, int limitEntity, int range) {
    List<Chunk> result = new ArrayList<>();
    World w = p.getWorld();
    int pcx = p.getLocation().getBlockX() >> 4;
    int pcz = p.getLocation().getBlockZ() >> 4;
    for (int x = pcx - range; x <= pcx + range; x++) {
      for (int z = pcz - range; z <= pcz + range; z++) {
        if (getDistance(pcx, pcz, x, z) > range) {
          continue;
        }
        if (!w.isChunkLoaded(x, z)) {
          continue;
        }
        Chunk c = w.getChunkAt(x, z);
        if (c.getTileEntities(false).length < limitEntity && c.getEntities().length < limitEntity) {
          continue;
        }
        result.add(c);
      }
    }
    return result;
  }

  /**
   * 集計を始められるか確認する。実行中の集計がある・クールダウン中なら、メッセージを出して false を返す。
   */
  private boolean canStartScan(CommandSender sender) {
    if (runningScan != null) {
      EntityAnalyzerUtility.sendMessage(sender, "ほかの集計が実行中です。終わってから実行してください。");
      return false;
    }
    Long until = cooldownMap.get(getSenderKey(sender));
    long now = System.currentTimeMillis();
    if (until != null && until > now) {
      long sec = (until - now + 999) / 1000;
      EntityAnalyzerUtility.sendMessage(sender, "クールダウン中です。あと " + sec + " 秒待ってから実行してください。");
      return false;
    }
    return true;
  }

  /**
   * 集計を始める。1 tick に scan_time_per_tick_ms ミリ秒ずつ数え、終わったら onFinish で結果を表示する。
   * 終わったあと、実行した人は scan_cooldown_seconds 秒間クールダウンになる。
   */
  private void startScan(CommandSender sender, List<Chunk> chunks, Consumer<Chunk> counter, Runnable onFinish) {
    String key = getSenderKey(sender);
    EntityAnalyzerUtility.sendMessage(sender, "集計を始めました。結果が出るまでお待ちください。");
    runningScan = new ChunkScanTask(chunks, getConfig().getLong("scan_time_per_tick_ms"), counter, onFinish, () -> {
      runningScan = null;
      long now = System.currentTimeMillis();
      cooldownMap.values().removeIf(t -> t <= now);
      cooldownMap.put(key, now + getConfig().getLong("scan_cooldown_seconds") * 1000L);
    });
    runningScan.runTaskTimer(this, 0L, 1L);
  }

  private String getSenderKey(CommandSender sender) {
    if (sender instanceof Player) {
      return ((Player) sender).getUniqueId().toString();
    }
    return sender.getName();
  }

  private World findWorld(CommandSender sender, String name) {
    World world = getServer().getWorld(name);
    if (world == null) {
      EntityAnalyzerUtility.sendMessage(sender, "ワールドが見つかりません: " + name);
    }
    return world;
  }

  private Integer parseNumber(CommandSender sender, String value) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      EntityAnalyzerUtility.sendMessage(sender, "数字で指定してください: " + value);
      return null;
    }
  }

  public List<Map.Entry<String, Integer>> getEntrySortedList(Map<String, Integer> rank) {
    List<Map.Entry<String, Integer>> entries = new ArrayList<Map.Entry<String, Integer>>(rank.entrySet());
    Collections.sort(entries, new Comparator<Map.Entry<String, Integer>>() {
      @Override
      public int compare(
          Entry<String, Integer> entry1, Entry<String, Integer> entry2) {
        return entry2.getValue().compareTo(entry1.getValue());
      }
    });
    return entries;
  }

  public int getDistance(int x1, int y1, int x2, int y2) {
    return (int) Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
  }

  public double getDistance(double x1, double z1, double x2, double z2) {
    return Math.sqrt(Math.pow((x2 - x1), 2) + Math.pow((z2 - z1), 2));
  }

  /**
   * JavaPlugin method onDisable.
   */
  @Override
  public void onDisable() {
    try {
      getLogger().info("The Plugin Has Been Disabled!");
    } catch (Exception e) {
      EntityAnalyzerUtility.logStackTrace(e);
    }
  }
}
