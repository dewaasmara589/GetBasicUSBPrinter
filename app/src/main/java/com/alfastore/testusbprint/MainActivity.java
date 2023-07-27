package com.alfastore.testusbprint;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.dantsu.escposprinter.EscPosPrinter;
import com.dantsu.escposprinter.connection.usb.UsbConnection;
import com.dantsu.escposprinter.exceptions.EscPosBarcodeException;
import com.dantsu.escposprinter.exceptions.EscPosConnectionException;
import com.dantsu.escposprinter.exceptions.EscPosEncodingException;
import com.dantsu.escposprinter.exceptions.EscPosParserException;
import com.dantsu.escposprinter.textparser.PrinterTextParserImg;

import java.util.HashMap;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity {

    private UsbManager mUsbManager;
    private UsbDevice mDevice;
    private UsbDeviceConnection mConnection;
    private UsbInterface mInterface;
    private UsbEndpoint mEndPoint;
    private PendingIntent mPermissionIntent;
    EditText ed_txt;
    Button print, printQRCode, printGambar;
    private static final String ACTION_USB_PERMISSION = "com.alfastore.testusbprint.USB_PERMISSION";
    private static Boolean forceCLaim = true;

    HashMap<String, UsbDevice> mDeviceList;
    Iterator<UsbDevice> mDeviceIterator;
    byte[] testBytes;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_main);
        super.onCreate(savedInstanceState);

        ed_txt = (EditText) findViewById(R.id.ed_txt);
        print = (Button) findViewById(R.id.print);
        printQRCode = (Button) findViewById(R.id.printQRCode);
        printGambar = (Button) findViewById(R.id.printGambar);

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        mDeviceList = mUsbManager.getDeviceList();

        if (mDeviceList.size() > 0) {
            mDeviceIterator = mDeviceList.values().iterator();

            TextView textView = (TextView) findViewById(R.id.usbDevice);
            textView.setMovementMethod(new ScrollingMovementMethod());
            String usbDevice = "";
            while (mDeviceIterator.hasNext()) {
                UsbDevice usbDevice1 = mDeviceIterator.next();
                if (usbDevice1.getVendorId() == 1221 && usbDevice1.getProductId() == 4718){
                    usbDevice += "\n" +
                            "DeviceID: " + usbDevice1.getDeviceId() + "\n" +
                            "DeviceName: " + usbDevice1.getDeviceName() + "\n" +
                            "Protocol: " + usbDevice1.getDeviceProtocol() + "\n" +
                            "Product Name: " + usbDevice1.getProductName() + "\n" +
                            "Manufacturer Name: " + usbDevice1.getManufacturerName() + "\n" +
                            "DeviceClass: " + usbDevice1.getDeviceClass() + " - " + translateDeviceClass(usbDevice1.getDeviceClass()) + "\n" +
                            "DeviceSubClass: " + usbDevice1.getDeviceSubclass() + "\n" +
                            "VendorID: " + usbDevice1.getVendorId() + "\n" +
                            "ProductID: " + usbDevice1.getProductId() + "\n";

                    Toast.makeText(this, "USB Printer FUJITSU Connected", Toast.LENGTH_SHORT).show();

                    mDevice = usbDevice1;

                    textView.setText(usbDevice);
                }
            }

            mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
            IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
            try {
                registerReceiver(mUsbReceiver, filter);
                mUsbManager.requestPermission(mDevice, mPermissionIntent);
            }catch (Exception e){
                Toast.makeText(getApplicationContext(), "ERROR " + e.toString(), Toast.LENGTH_LONG).show();
            }

        } else {
            Toast.makeText(this, "Please attach printer via USB", Toast.LENGTH_SHORT).show();
        }
        print.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                print(mConnection, mInterface);
            }
        });

        printQRCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                printQRCode(mConnection, mInterface);
            }
        });

        printGambar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                printGambar(mConnection, mInterface);
            }
        });
    }

    private void print(final UsbDeviceConnection connection, final UsbInterface usbInterface) {
        final String test = ed_txt.getText().toString() + "\n";
        testBytes = test.getBytes();

        if (usbInterface == null) {
            Toast.makeText(this, "INTERFACE IS NULL", Toast.LENGTH_SHORT).show();
        } else if (connection == null) {
            Toast.makeText(this, "CONNECTION IS NULL", Toast.LENGTH_SHORT).show();
        } else if (forceCLaim == null) {
            Toast.makeText(this, "FORCE CLAIM IS NULL", Toast.LENGTH_SHORT).show();
        } else {

            connection.claimInterface(usbInterface, forceCLaim);

            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    connection.bulkTransfer(mEndPoint, testBytes, testBytes.length, 0);
                }
            });
            thread.run();

            cutPaper(connection, usbInterface);
        }
    }

    private void printQRCode(final UsbDeviceConnection connection, final UsbInterface usbInterface) {
//        final String test = ed_txt.getText().toString() + "\n";

        if (usbInterface == null) {
            Toast.makeText(this, "INTERFACE IS NULL", Toast.LENGTH_SHORT).show();
        } else if (connection == null) {
            Toast.makeText(this, "CONNECTION IS NULL", Toast.LENGTH_SHORT).show();
        } else if (forceCLaim == null) {
            Toast.makeText(this, "FORCE CLAIM IS NULL", Toast.LENGTH_SHORT).show();
        } else {

            connection.claimInterface(usbInterface, forceCLaim);

            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        EscPosPrinter printer = new EscPosPrinter(new UsbConnection(mUsbManager, mDevice), 203, 46f, 40);
                        printer.printFormattedText(
                                "[C]<qrcode size='25'>tes</qrcode>\n"
                        );
                    } catch (EscPosConnectionException e) {
                        throw new RuntimeException(e);
                    } catch (EscPosParserException e) {
                        throw new RuntimeException(e);
                    } catch (EscPosEncodingException e) {
                        throw new RuntimeException(e);
                    } catch (EscPosBarcodeException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            thread.run();

            cutPaper(connection, usbInterface);
        }
    }

    private void printGambar(final UsbDeviceConnection connection, final UsbInterface usbInterface) {
        if (usbInterface == null) {
            Toast.makeText(this, "INTERFACE IS NULL", Toast.LENGTH_SHORT).show();
        } else if (connection == null) {
            Toast.makeText(this, "CONNECTION IS NULL", Toast.LENGTH_SHORT).show();
        } else if (forceCLaim == null) {
            Toast.makeText(this, "FORCE CLAIM IS NULL", Toast.LENGTH_SHORT).show();
        } else {

            connection.claimInterface(usbInterface, forceCLaim);

            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        EscPosPrinter printer = new EscPosPrinter(new UsbConnection(mUsbManager, mDevice), 203, 46f, 40);
                        printer.printFormattedText(
                                "[C]<img>" + PrinterTextParserImg.bitmapToHexadecimalString(printer, getApplicationContext().getResources().getDrawableForDensity(R.drawable.ic_report, DisplayMetrics.DENSITY_MEDIUM)) + "</img>\n"
                        );
                    } catch (EscPosConnectionException e) {
                        throw new RuntimeException(e);
                    } catch (EscPosParserException e) {
                        throw new RuntimeException(e);
                    } catch (EscPosEncodingException e) {
                        throw new RuntimeException(e);
                    } catch (EscPosBarcodeException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            thread.run();

            cutPaper(connection, usbInterface);
        }
    }

    private void cutPaper(final UsbDeviceConnection connection, final UsbInterface usbInterface) {
        if (usbInterface == null) {
            Toast.makeText(this, "INTERFACE IS NULL", Toast.LENGTH_SHORT).show();
        } else if (connection == null) {
            Toast.makeText(this, "CONNECTION IS NULL", Toast.LENGTH_SHORT).show();
        } else if (forceCLaim == null) {
            Toast.makeText(this, "FORCE CLAIM IS NULL", Toast.LENGTH_SHORT).show();
        } else {

            connection.claimInterface(usbInterface, forceCLaim);

            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    byte[] cut_paper = {0x1D, 0x56, 0x41, 0x10};
                    connection.bulkTransfer(mEndPoint, cut_paper, cut_paper.length, 0);
                }
            });
            thread.run();
        }
    }


    final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            //call method to set up device communication
                            mInterface = device.getInterface(0);
                            mEndPoint = mInterface.getEndpoint(0);// 0 IN and  1 OUT to printer.
                            mConnection = mUsbManager.openDevice(device);

                        }
                    } else {
                        Toast.makeText(context, "PERMISSION DENIED FOR THIS DEVICE", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    };


    private String translateDeviceClass(int deviceClass) {

        switch (deviceClass) {

            case UsbConstants.USB_CLASS_APP_SPEC:
                return "Application specific USB class";

            case UsbConstants.USB_CLASS_AUDIO:
                return "USB class for audio devices";

            case UsbConstants.USB_CLASS_CDC_DATA:
                return "USB class for CDC devices (communications device class)";

            case UsbConstants.USB_CLASS_COMM:
                return "USB class for communication devices";

            case UsbConstants.USB_CLASS_CONTENT_SEC:
                return "USB class for content security devices";

            case UsbConstants.USB_CLASS_CSCID:
                return "USB class for content smart card devices";

            case UsbConstants.USB_CLASS_HID:
                return "USB class for human interface devices (for example, mice and keyboards)";

            case UsbConstants.USB_CLASS_HUB:
                return "USB class for USB hubs";

            case UsbConstants.USB_CLASS_MASS_STORAGE:
                return "USB class for mass storage devices";

            case UsbConstants.USB_CLASS_MISC:
                return "USB class for wireless miscellaneous devices";

            case UsbConstants.USB_CLASS_PER_INTERFACE:
                return "USB class indicating that the class is determined on a per-interface basis";

            case UsbConstants.USB_CLASS_PHYSICA:
                return "USB class for physical devices";

            case UsbConstants.USB_CLASS_PRINTER:
                return "USB class for printers";

            case UsbConstants.USB_CLASS_STILL_IMAGE:
                return "USB class for still image devices (digital cameras)";

            case UsbConstants.USB_CLASS_VENDOR_SPEC:
                return "Vendor specific USB class";

            case UsbConstants.USB_CLASS_VIDEO:
                return "USB class for video devices";

            case UsbConstants.USB_CLASS_WIRELESS_CONTROLLER:
                return "USB class for wireless controller devices";

            default:
                return "Unknown USB class!";
        }
    }

}