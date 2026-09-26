package com.example.termprinter.printer;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class HtmlToImageConverter {

    // Интерфейс обратного вызова (Callback) для получения результата
    public interface ConversionCallback {
        void onResult(File outputFile);
        void onError(Exception e);
    }

    public static void convertHtmlToPng(final Context context,
                                        final String htmlString,
                                        final int width,
                                        final int height,
                                        final ConversionCallback callback) {

        // Вся работа с WebView в Android должна происходить строго в Главном (UI) потоке
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                final WebView webView = new WebView(context);

                // Задаем размеры "виртуального экрана" для рендеринга
                webView.layout(0, 0, width, height);

                webView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        super.onPageFinished(view, url);

                        // Небольшая задержка (100мс), чтобы шрифты и стили успели примениться
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Bitmap bitmap = null;
                                FileOutputStream out = null;
                                try {
                                    // 1. Создаем пустую картинку в памяти с поддержкой прозрачности
                                    bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

                                    // 2. Создаем холст (Canvas) на основе этой картинки
                                    Canvas canvas = new Canvas(bitmap);

                                    // 3. Отрисовываем содержимое WebView на холст
                                    webView.draw(canvas);

                                    // 4. Определяем файл во внутреннем кэше приложения (права не нужны)
                                    File outputFile = new File(context.getCacheDir(), "rendered_html.png");

                                    out = new FileOutputStream(outputFile);
                                    // Сжимаем Bitmap в формат PNG со 100% качеством
                                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);

                                    // Возвращаем успешный результат
                                    if (callback != null) {
                                        callback.onResult(outputFile);
                                    }

                                } catch (Exception e) {
                                    Log.e("HtmlToPng", "Ошибка при конвертации", e);
                                    if (callback != null) {
                                        callback.onError(e);
                                    }
                                } finally {
                                    // Обязательно освобождаем ресурсы
                                    if (bitmap != null) {
                                        bitmap.recycle();
                                    }
                                    if (out != null) {
                                        try {
                                            out.close();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }
                        }, 100); // 100 миллисекунд задержки
                    }
                });

                // Загружаем HTML строку в WebView
                webView.loadDataWithBaseURL(null, htmlString, "text/html", "utf-8", null);
            }
        });
    }
    public static File savePngToPublicStorageAndroid9(Context context, File cachedFile, String fileName) {
        // 1. Получаем путь к публичной папке Pictures
        File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        // 2. Создаем нашу подпапку HtmlToPng, если её еще нет
        File myDir = new File(picturesDir, "HtmlToPng");
        if (!myDir.exists()) {
            myDir.mkdirs();
        }

        // 3. Целевой файл
        File publicFile = new File(myDir, fileName + ".png");

        // 4. Побайтово копируем файл из кэша приложения в общую память
        try (FileInputStream in = new FileInputStream(cachedFile);
             FileOutputStream out = new FileOutputStream(publicFile)) {

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }

            Log.d("STORAGE", "Файл физически скопирован: " + publicFile.getAbsolutePath());

            // 5. Важно для Android 9: принудительно уведомляем Медиа-Сканер системы,
            // чтобы картинка МГНОВЕННО появилась в Галерее и Проводнике
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri contentUri = Uri.fromFile(publicFile);
            mediaScanIntent.setData(contentUri);
            context.sendBroadcast(mediaScanIntent);

            return publicFile;

        } catch (Exception e) {
            Log.e("STORAGE", "Ошибка записи в общую память", e);
            return null;
        }
    }
}