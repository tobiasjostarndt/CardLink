package com.appdinx.cardlink.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.appdinx.cardlink.R;

public class CanInsertionActivity extends AppCompatActivity {
    private EditText digit1, digit2, digit3, digit4, digit5, digit6;
    private Button continueButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_can_insert);

        digit1 = findViewById(R.id.phone_number);
        digit2 = findViewById(R.id.digit2);
        digit3 = findViewById(R.id.digit3);
        digit4 = findViewById(R.id.digit4);
        digit5 = findViewById(R.id.digit5);
        digit6 = findViewById(R.id.digit6);

        setupTextWatchers();

        continueButton = findViewById(R.id.continue_button);
        continueButton.setEnabled(false);
        continueButton.setOnClickListener(v -> {
            // Get the entered digits
            String digit1Value = digit1.getText().toString();
            String digit2Value = digit2.getText().toString();
            String digit3Value = digit3.getText().toString();
            String digit4Value = digit4.getText().toString();
            String digit5Value = digit5.getText().toString();
            String digit6Value = digit6.getText().toString();
            // Start the CardInsertionActivity
            Intent intent = new Intent(CanInsertionActivity.this, CardInsertionActivity.class);
            intent.putExtra("digit1", digit1Value);
            intent.putExtra("digit2", digit2Value);
            intent.putExtra("digit3", digit3Value);
            intent.putExtra("digit4", digit4Value);
            intent.putExtra("digit5", digit5Value);
            intent.putExtra("digit6", digit6Value);
            startActivity(intent);
        });

        // Add any additional logic or functionality here

        setupPreviousButton();
    }

    private void setupPreviousButton() {
        // Go back to PhoneInsertionActivity
        Button previousButton = findViewById(R.id.previous_button);
        previousButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, PhoneInsertionActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        nfcAdapter.disableReaderMode(this);
    }

    private void setupTextWatchers() {
        digit1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1) {
                    digit2.requestFocus();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                enableContinueButtonIfDigitsEntered();
            }
        });

        digit2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1) {
                    digit3.requestFocus();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                enableContinueButtonIfDigitsEntered();
            }
        });

        digit3.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1) {
                    digit4.requestFocus();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                enableContinueButtonIfDigitsEntered();
            }
        });

        digit4.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1) {
                    digit5.requestFocus();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                enableContinueButtonIfDigitsEntered();
            }
        });

        digit5.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1) {
                    digit6.requestFocus();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                enableContinueButtonIfDigitsEntered();
            }
        });

        digit6.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // No implementation needed
            }

            @Override
            public void afterTextChanged(Editable s) {
                enableContinueButtonIfDigitsEntered();

                if (s.length() == 1) {
                    // Hide the keyboard
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(digit6.getWindowToken(), 0);
                }
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DEL) {
            EditText focusedEditText = getFocusedEditText();
            if (focusedEditText != null) {
                int previousDigitIndex = getPreviousDigitIndex(focusedEditText);
                if (previousDigitIndex >= 0) {
                    EditText previousDigitEditText = getDigitEditTextByIndex(previousDigitIndex);
                    assert previousDigitEditText != null;
                    previousDigitEditText.requestFocus();
                    previousDigitEditText.setText("");
                }
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private EditText getFocusedEditText() {
        EditText[] digitEditTexts = {digit1, digit2, digit3, digit4, digit5, digit6};

        for (EditText editText : digitEditTexts) {
            if (editText.hasFocus()) {
                return editText;
            }
        }

        return null;
    }

    private int getPreviousDigitIndex(EditText editText) {
        if (editText == digit2) {
            return 1;
        } else if (editText == digit3) {
            return 2;
        } else if (editText == digit4) {
            return 3;
        } else if (editText == digit5) {
            return 4;
        } else if (editText == digit6) {
            return 5;
        }
        return -1;
    }

    private EditText getDigitEditTextByIndex(int index) {
        switch (index) {
            case 1:
                return digit1;
            case 2:
                return digit2;
            case 3:
                return digit3;
            case 4:
                return digit4;
            case 5:
                return digit5;
            case 6:
                return digit6;
            default:
                return null;
        }
    }

    private void enableContinueButtonIfDigitsEntered() {
        boolean digitsEntered = isDigitEntered(digit1) &&
                isDigitEntered(digit2) &&
                isDigitEntered(digit3) &&
                isDigitEntered(digit4) &&
                isDigitEntered(digit5) &&
                isDigitEntered(digit6);

        continueButton.setEnabled(digitsEntered);

        if (digitsEntered) {
            continueButton.setBackgroundColor(Color.parseColor("#00463D"));
        }
    }

    private boolean isDigitEntered(EditText editText) {
        String digit = editText.getText().toString().trim();
        return !digit.isEmpty();
    }
}
