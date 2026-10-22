package space.gorogoro.entityanalyzer;

import java.util.List;
import java.util.function.Consumer;

import org.bukkit.Chunk;
import org.bukkit.scheduler.BukkitRunnable;

/*
 * ChunkScanTask
 * @license    LGPLv3
 * @copyright  Copyright gorogoro.space 2017
 * @author     kubotan
 * @see        <a href="http://blog.gorogoro.space">Kubotan's blog.</a>
 */
public class ChunkScanTask extends BukkitRunnable {

  private final List<Chunk> chunks;
  private final long timePerTickNanos;
  private final Consumer<Chunk> counter;
  private final Runnable onFinish;
  private final Runnable onEnd;
  private int index = 0;

  /**
   * 1 tick に timePerTickMillis ミリ秒だけチャンクを数え、続きは次の tick に回す。
   * @param chunks 数えるチャンク
   * @param timePerTickMillis 1 tick に数える時間(ミリ秒)
   * @param counter 1 チャンク分を数える処理
   * @param onFinish 数え終わったときの処理(結果の表示)
   * @param onEnd 終了時に必ず行う処理(途中で失敗したときも呼ぶ)
   */
  public ChunkScanTask(List<Chunk> chunks, long timePerTickMillis, Consumer<Chunk> counter, Runnable onFinish,
      Runnable onEnd) {
    this.chunks = chunks;
    this.timePerTickNanos = timePerTickMillis * 1000000L;
    this.counter = counter;
    this.onFinish = onFinish;
    this.onEnd = onEnd;
  }

  @Override
  public void run() {
    try {
      long start = System.nanoTime();
      // 最低 1 チャンクは数えて、決めた時間を過ぎたら次の tick に回す
      do {
        if (index >= chunks.size()) {
          cancel();
          try {
            onFinish.run();
          } finally {
            onEnd.run();
          }
          return;
        }
        Chunk c = chunks.get(index++);
        // 途中でアンロードされたチャンクは、読み込み直さずに飛ばす
        if (c.isLoaded()) {
          counter.accept(c);
        }
      } while (System.nanoTime() - start < timePerTickNanos);
    } catch (Exception e) {
      if (!isCancelled()) {
        cancel();
        onEnd.run();
      }
      EntityAnalyzerUtility.logStackTrace(e);
    }
  }
}
