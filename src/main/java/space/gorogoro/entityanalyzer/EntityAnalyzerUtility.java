package space.gorogoro.entityanalyzer;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/*
 * EntityAnalyzerUtility
 * @license    LGPLv3
 * @copyright  Copyright gorogoro.space 2017
 * @author     kubotan
 * @see        <a href="http://blog.gorogoro.space">Kubotan's blog.</a>
 */
public class EntityAnalyzerUtility {
  
  /**
   * Output stack trace to log file.
   * @param Exception Exception
   */
  public static void logStackTrace(Exception e){
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      e.printStackTrace(pw);
      pw.flush();
      Bukkit.getLogger().log(Level.WARNING, sw.toString());
  }
  
  /**
   * Send message to player
   * @param CommandSender CommandSender
   * @param String message
   */
  public static void sendMessage(CommandSender sender, String message){
    // 集計の結果が出る前にログアウトしたプレイヤーには送らない
    if (sender instanceof Player && !((Player) sender).isOnline()) {
      return;
    }
    sender.sendMessage(Component.text("[EntityAnalyzer]", NamedTextColor.DARK_RED)
        .append(Component.text(" " + message, NamedTextColor.RED)));
  }
}