package com.example.termprinter.printer;

import android.os.Bundle;
import android.os.RemoteException;
import android.view.View;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.xzy.pos.sdk.print.PrinterManager;
import com.example.termprinter.R;

public class TextPrinter extends AppCompatActivity {
    PrinterManager printer;
    EditText editText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.text_printer);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        editText = findViewById(R.id.simpleEditText);
        printer = new PrinterManager();

    }
    /*
    public void printReceipt() {
        // Запускаем печать в фоновом потоке
        new Thread(new Runnable() {
            @Override
            public void run() {
                // 1. Создаем экземпляр менеджера (он автоматически очистит кэш при создании)
                PrinterManager printer = new PrinterManager();

                try {
                    // 2. Проверяем наличие бумаги перед началом работы
                    if (!printer.queryIfHavePaper()) {
                        System.out.println("Ошибка: Нет бумаги в принтере!");
                        return;
                    }

                    // 3. Открываем порт/соединение с принтером
                    int openResult = PrinterManager.open();
                    // Если open() возвращает код ошибки (например, ERROR_BUSY)
                    if (openResult == PrinterManager.ERROR_BUSY) {
                        System.out.println("Принтер занят другой задачей");
                        return;
                    }

                    // 4. Формируем чек (используем методы экземпляра printer)

                    // Заголовок: Крупный, Жирный, по Центру
                    printer.addLine("ХУЙ", true, false, false, PrinterManager.FONT_LARGE, PrinterManager.CENTER);

                    // Добавление QR-кода (например, ссылка на чек или СБП)
                    // Текст, Размер (пиксели), Позиция (Центр)


                    // 5. Прокатка ленты в конце (чтобы чек вылез из прорези и его можно было оторвать)
                    // Метод статический, принимает количество шагов/линий прокрутки
                    PrinterManager.PrintStep(6);

                    // 6. Отправляем сформированный буфер на физическую печать
                    int printStatus = printer.beginPrint();

                    if (printStatus == 0) { // Обычно 0 означает STATUS_OK
                        System.out.println("Печать успешно завершена");
                    } else {
                        System.out.println("Ошибка печати. Код: " + printStatus);
                    }

                } catch (RemoteException e) {
                    e.printStackTrace();
                    System.out.println("Критическая ошибка связи с сервисом принтера");
                } finally {
                    // 7. Обязательно закрываем принтер, чтобы освободить ресурс для других приложений
                    try {
                        PrinterManager.close();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
    */
    public void goPrint(View view)  {
        String text = editText.getText().toString();
        try {
            int open = PrinterManager.open();

            printer.addLine(text, false, false, false, 14 , PrinterManager.CENTER);
            printer.beginPrint();
        }catch (RemoteException e){
            System.out.println(e);
        }



    }
}
