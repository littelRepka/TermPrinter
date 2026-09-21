package com.example.termprinter.printer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.documentfile.provider.DocumentFile;

import com.example.termprinter.R;
import com.xzy.pos.sdk.print.PrinterManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class PhotoPrinter extends AppCompatActivity {
    PrinterManager printer;
    byte[] bytes;
    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.photo_printer);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.p_printer), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        printer = new PrinterManager();
    }

        // 1. Лаунчер для выбора ОДНОГО файла (работает везде)
        private final ActivityResultLauncher<String[]> filePicker = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) {
                        try (InputStream is = getContentResolver().openInputStream(uri)) {
                            bytes = readAllBytesCompat(is);


                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Ошибка чтения файла", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        // 2. Метод, который указан в xml как android:onClick="onPickFileClick"
        // ВАЖНО: он должен быть public, возвращать void и принимать View
        public void Print(View view) {
            try {
                filePicker.launch(new String[]{"image/png", "image/bmp"});
            } catch (Exception e) {
                Toast.makeText(this, "Не удалось открыть файловый менеджер", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }

        // 3. Вспомогательный метод для чтения байт (для всех API)
        private byte[] readAllBytesCompat(InputStream is) throws IOException {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, bytesRead);
            }
            return buffer.toByteArray();
        }

        public void Clic(View view){
            try {
                int open = PrinterManager.open();
                //printer.addLine("ХУЙ", true, true, false, 40 , PrinterManager.LEFT);
                printer.addBitmap(bytes, 1, 0);
                printer.beginPrint();

                PrinterManager.close();
            }catch (RemoteException e){
                System.out.println(e);
            }
        }


}