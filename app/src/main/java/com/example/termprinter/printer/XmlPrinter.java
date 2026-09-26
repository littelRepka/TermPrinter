package com.example.termprinter.printer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.termprinter.R;
import com.example.termprinter.printer.HtmlToImageConverter;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

public class XmlPrinter extends AppCompatActivity {

    private static final int REQUEST_CODE_WRITE_STORAGE = 101;
    private String loadedHtmlContent = ""; // Здесь будет храниться текст выбранного HTML

    // 1. Регистрируем контракт для выбора файла из системы
    private final ActivityResultLauncher<String> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    // Пользователь выбрал файл, читаем его содержимое
                    readHtmlFromUri(uri);
                } else {
                    Toast.makeText(this, "Файл не выбран", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.xml_printer);

        // Кнопка для открытия проводника
        Button btnPickFile = findViewById(R.id.btn_pick_file);
        btnPickFile.setOnClickListener(v -> {
            // Запрашиваем только файлы с MIME-типом text/html
            filePickerLauncher.launch("text/html");
        });

        // Кнопка для запуска конвертации
        Button btnConvert = findViewById(R.id.btn_convert);
        btnConvert.setOnClickListener(v -> {
            if (loadedHtmlContent.isEmpty()) {
                Toast.makeText(this, "Сначала выберите HTML файл!", Toast.LENGTH_SHORT).show();
                return;
            }
            checkPermissionAndSave();
        });
    }

    // 2. Чтение содержимого файла через ContentResolver по его Uri
    private void readHtmlFromUri(Uri uri) {
        StringBuilder stringBuilder = new StringBuilder();
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }

            loadedHtmlContent = stringBuilder.toString();
            Toast.makeText(this, "HTML файл успешно загружен!", Toast.LENGTH_SHORT).show();
            Log.d("HTML_PICKER", "Загруженный HTML:\n" + loadedHtmlContent);

        } catch (Exception e) {
            Log.e("HTML_PICKER", "Ошибка чтения файла", e);
            Toast.makeText(this, "Не удалось прочитать файл", Toast.LENGTH_SHORT).show();
        }
    }

    // 3. Проверка разрешений для Android 9 перед сохранением PNG
    private void checkPermissionAndSave() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            startHtmlConversion();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CODE_WRITE_STORAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_WRITE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startHtmlConversion();
            } else {
                Toast.makeText(this, "Без разрешения нельзя сохранить файл в память!", Toast.LENGTH_LONG).show();
            }
        }
    }

    // 4. Запуск генерации картинки
    private void startHtmlConversion() {
        HtmlToImageConverter.convertHtmlToPng(this, loadedHtmlContent, 800, 600, new HtmlToImageConverter.ConversionCallback() {
            @Override
            public void onResult(File outputFile) {
                String name = "picked_html_" + System.currentTimeMillis();
                File savedFile = HtmlToImageConverter.savePngToPublicStorageAndroid9(XmlPrinter.this, outputFile, name);

                if (savedFile != null) {
                    runOnUiThread(() -> Toast.makeText(XmlPrinter.this,
                            "Сохранено в Pictures/HtmlToPng!", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> Toast.makeText(XmlPrinter.this, "Ошибка рендеринга HTML", Toast.LENGTH_SHORT).show());
            }
        });
    }
}