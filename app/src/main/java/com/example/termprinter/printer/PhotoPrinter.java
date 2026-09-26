package com.example.termprinter.printer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.termprinter.R;
import com.xzy.pos.sdk.print.PrinterManager;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;




public class PhotoPrinter extends AppCompatActivity {
    PrinterManager printer;
    byte[] bytes;
    private Uri photoUri;
    private File photoFile;

    boolean mode = true;

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
                        if(mode) {
                            // 1. Декодируем поток в изменяемый (mutable) Bitmap
                            BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inMutable = true; // КРИТИЧЕСКИ ВАЖНО для редактирования
                            Bitmap bitmap = BitmapFactory.decodeStream(is, null, options);

                            if (bitmap != null) {
                                // 2. Редактируем пиксели
                                editPixels(bitmap);

                                // 3. (Опционально) Сохраняем измененный Bitmap обратно в byte[]
                                ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
                                bytes = outStream.toByteArray();

                                // Log.d("PIXEL_EDIT", "Готово! Новый размер: " + modifiedBytes.length + " байт");

                                // TODO: Делай что нужно с modifiedBytes или bitmap
                            }
                        }else{
                            bytes = readAllBytesCompat(is);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
    );

    // Метод для быстрого редактирования пикселей
    private void editPixels(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        // Получаем все пиксели в одномерный массив (гораздо быстрее, чем getPixel/setPixel в цикле)
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);


        // Проходим по всем пикселям
        for (int i = 1; i < pixels.length; i+=2) {
            int pixel = pixels[i];
            int last_pixel = pixels[i-1];

            // Извлекаем каналы цвета (ARGB)

            int r1 = android.graphics.Color.red(last_pixel);
            int g1 = android.graphics.Color.green(last_pixel);
            int b1 = android.graphics.Color.blue(last_pixel);

            int r2 = android.graphics.Color.red(pixel);
            int g2 = android.graphics.Color.green(pixel);
            int b2 = android.graphics.Color.blue(pixel);


            double Y1 = (r1 * 0.299) + (g1 * 0.587) + (b1 * 0.114);
            double Y2 = (r2 * 0.299) + (g2 * 0.587) + (b2 * 0.114);
            int v = 0;
            if ((Y2+Y1) > 200){
                r1 =255;
                g1= 255;
                b1= 255;
                r2 =255;
                g2= 255;
                b2= 255;
            }else if((Y1+Y2) > 200 && (Y1+Y2) < 400){
                r1 =0;
                g1= 0;
               b1= 0;
               r2 =255;
                g2= 255;
                b2= 255;
            }else {
                r1 =0;
                g1= 0;
                b1= 0;
                r2 =0;
                g2= 0;
                b2= 0;
            }

            // ----------------------------------------------------

            // Собираем пиксель обратно и записываем в массив
            pixels[i] = android.graphics.Color.argb(0, r2, g2, b2);
            pixels[i-1] = android.graphics.Color.argb(0, r1, g1, b1);
           // last_pixel =pixel;

        }

        // Записываем измененный массив обратно в Bitmap
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
    }


    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    launchCamera(); // Разрешение дали, запускаем камеру
                } else {
                    Toast.makeText(this, "Нужно разрешение на использование камеры", Toast.LENGTH_SHORT).show();
                }
            });
    private byte[] convertBit(Bitmap bitmap){
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        int[] allPixels = new int[width * height];
        byte[] rezult = new byte[width * height];

        bitmap.getPixels(allPixels, 0, width, 0, 0, width, height);
        for (int i = 0 ; i < width*height; i++ ){
            int k =  allPixels[i];
            int k1 = ((k >> 8) & 0xFF);
            int k2 = ((k >> 8) & 0xFF);
            int k3 = ((k >> 8) & 0xFF);
            if (k1+k2+k3 <90){
                rezult[i] = 0;
            }else{
                rezult[i] =8;
            }
        }
        return rezult;
    }
    private void launchCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            try {
                photoFile = createImageFile();
                //File outputBmp = null;



                        // ВАЖНО: "com.example.termprinter.fileprovider" должен совпадать с authorities в Manifest!
                photoUri = FileProvider.getUriForFile(this,
                        getPackageName() + ".fileprovider", photoFile);

                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                cameraLauncher.launch(takePictureIntent);
               // FileInputStream fis = new FileInputStream(photoFile);
                //FileOutputStream fos = new FileOutputStream(outputBmp);

                // Декодируем поток в Bitmap
                //Bitmap bitmap = BitmapFactory.decodeStream(fis);
                //bytes = convertBit(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Ошибка создания файла", Toast.LENGTH_SHORT).show();
            }
        }


    }

    // 5. Создание временного файла для фото
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    // 2. Лаунчер для запуска КАМЕРЫ
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (photoFile != null && photoFile.exists()) {
                        try {
                            // Получаем байты из сохраненного файла
                            byte[] bytes = readAllBytesCompat(new FileInputStream(photoFile));
                            Toast.makeText(this, "Фото получено! Байт: " + bytes.length, Toast.LENGTH_SHORT).show();

                            // TODO: Делай что нужно с массивом bytes

                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Ошибка чтения файла", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });



        public void Print(View view) {
            mode =true;
            try {
                filePicker.launch(new String[]{"image/png", "image/bmp"});
            } catch (Exception e) {
                Toast.makeText(this, "Не удалось открыть файловый менеджер", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }

        public void PngPrint(View view){
            mode =false;
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
               // printer.addLine("ХУЙ", true, true, false, 40 , PrinterManager.LEFT);


                //Log.d("COPY_FILE", "Файл создан. Размер: " + bytes.length);
                printer.addBitmap(bytes, 1, 0);

                printer.beginPrint();
                printer.cleanCache();
               // PrinterManager.PrintStep(20);
                PrinterManager.close();
            }catch (RemoteException e){
                System.out.println(e);
            }
        }
        public void Photo(View view){
            // Проверяем, есть ли уже разрешение
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                // Запрашиваем разрешение у пользователя
                requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        }


}