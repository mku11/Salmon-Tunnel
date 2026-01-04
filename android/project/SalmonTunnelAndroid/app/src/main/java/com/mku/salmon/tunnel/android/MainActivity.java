package com.mku.salmon.tunnel.android;
/*
MIT License

Copyright (c) 2025 Max Kas

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mku.salmon.tunnel.android.databinding.ActivityMainBinding;

import java.net.SocketException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private static boolean running;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toggle.setOnClickListener((e) -> {
            toggle();
        });
        binding.host.setOnEditorActionListener((v, pos, e) -> {
            if (v.getText().toString().length() > 0) {
                binding.serviceDevice.setChecked(false);
            } else {
                binding.serviceDevice.setChecked(true);
            }
            return true;
        });

        TunnelService.setStatusEventListener((event, msg) -> {

            if (event == TunnelService.StatusEvent.STARTED) {
                binding.toggle.post(() -> {
                    binding.toggle.setImageResource(R.drawable.on);
                });

                ArrayList<String> hostsAndPorts = null;
                try {
                    hostsAndPorts = TunnelService.getIPv4sAndPorts();
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < hostsAndPorts.size(); ++i) {
                        sb.append(hostsAndPorts.get(i));
                        if (i != hostsAndPorts.size() - 1)
                            sb.append(" ");
                    }
                    binding.address.post(() -> {
                        binding.address.setText(getString(R.string.salmon_tunnel_running_at, sb.toString()));
                    });
                } catch (SocketException e) {
                    throw new RuntimeException(e);
                }
                enableUi(false);
                running = true;
            } else if (event == TunnelService.StatusEvent.STOPPED) {
                binding.toggle.post(() -> {
                    binding.toggle.setImageResource(R.drawable.off);
                });
                binding.address.post(() -> {
                    binding.address.setText("");
                });
                enableUi(true);
                running = false;
            } else if (event == TunnelService.StatusEvent.ERROR) {
				runOnUiThread(()-> {
					Toast.makeText(this, "Error: " + msg, Toast.LENGTH_LONG).show();
				});
			}
        });
        loadSettings();
    }

    private void enableUi(boolean value) {
        binding.sourcePort.setEnabled(value);
        binding.targetPort.setEnabled(value);
        binding.host.setEnabled(value);
        binding.serviceDevice.setEnabled(value);
        binding.password.setEnabled(value);
        binding.bufferSize.setEnabled(value);
    }

    private void toggle() {
        saveSettings();
        Intent intent = new Intent(MainActivity.this, TunnelService.class);
        if (running)
            intent.setAction(TunnelService.ACTION_STOP);
        else
            intent.setAction(TunnelService.ACTION_START);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }


    private void loadSettings() {
        int sourcePort = Settings.getSourcePort();
        if (sourcePort > 0)
            binding.sourcePort.setText(Integer.toString(sourcePort));
        int targetPort = Settings.getTargetPort();
        if (targetPort > 0)
            binding.targetPort.setText(Integer.toString(targetPort));
        binding.host.setText(Settings.getHost());
        binding.password.setText(Settings.getPassword());
        binding.serviceDevice.setChecked(Settings.getServiceDevice());
        binding.bufferSize.setValue(Settings.getBufferSize());
    }

    private void saveSettings() {
        Settings.setSourcePort(Integer.parseInt(binding.sourcePort.getText().toString()));
        Settings.setTargetPort(Integer.parseInt(binding.targetPort.getText().toString()));
        Settings.setHost(binding.host.getText().toString());
        Settings.setPassword(binding.password.getText().toString());
        Settings.setServiceDevice(binding.serviceDevice.isChecked());
        Settings.setBufferSize((int) binding.bufferSize.getValue());
    }
}