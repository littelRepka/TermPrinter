package com.example.termprinter.printer;

import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

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
    ProgressBar progressBar;
    TextView textView;
    Button print;
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
        progressBar =findViewById(R.id.progressBar);
        textView = findViewById(R.id.textView);
        print =findViewById(R.id.print);


    }
    public  void pause(boolean is){
        if (is){
            progressBar.setVisibility(View.VISIBLE);
            textView.setVisibility(View.VISIBLE);
            print.setEnabled(false);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        }else {
            progressBar.setVisibility(View.GONE);
            textView.setVisibility(View.GONE);
            print.setEnabled(true);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        }
    }
    public void goPrint(View view)  {
        String text = editText.getText().toString();
        try {
            int open = PrinterManager.open();
            pause(true);
            printer.addLine(text, false, false, false, 20 , PrinterManager.LEFT);
            printer.beginPrint();
            pause(false);
            PrinterManager.close();
        }catch (RemoteException e){
            System.out.println(e);
        }



    }
}
