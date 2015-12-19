package tk.yvbb.yv3daudio;

import java.io.File;

import com.un4seen.bass.BASS;

import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ToggleButton;

public class MainActivity extends ActionBarActivity {

    int chan;
    File filepath;
    String[] filelist;
    yvHRTF hrtf;
    Button openbut,stopbut,applybut;
    ToggleButton playtb,cycletb;
    EditText xtext,ytext,ztext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
        BASS.BASS_Init(-1,44100,0);
        filepath=Environment.getExternalStorageDirectory();
        openbut=(Button)findViewById(R.id.button1);
        playtb=(ToggleButton)findViewById(R.id.toggleButton1);
        stopbut=(Button)findViewById(R.id.button2);
        xtext=(EditText)findViewById(R.id.editText1);
        ytext=(EditText)findViewById(R.id.editText2);
        ztext=(EditText)findViewById(R.id.editText3);
        applybut=(Button)findViewById(R.id.button3);
        cycletb=(ToggleButton)findViewById(R.id.toggleButton2);
    }

    public void OpenClick(View v) {
        String[] list=filepath.list();
        if (list==null) list=new String[0];
        if (!filepath.getPath().equals("/")) {
            filelist=new String[list.length+1];
            filelist[0]="..";
            System.arraycopy(list, 0, filelist, 1, list.length);
        } else
            filelist=list;
        new AlertDialog.Builder(this)
            .setTitle("Choose a file to play")
            .setItems(filelist, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    File sel;
                    if (filelist[which].equals("..")) sel=filepath.getParentFile();
                    else sel=new File(filepath, filelist[which]);
                    if (sel.isDirectory()) {
                        filepath=sel;
                        OpenClick(null);
                    } else {
                        String file=sel.getPath();
                        if (hrtf!=null)
                            hrtf.Stop3D();
                        if (!BASS.BASS_StreamFree(chan))
                            BASS.BASS_MusicFree(chan);
                        if ((chan=BASS.BASS_StreamCreateFile(file, 0, 0, BASS.BASS_SAMPLE_LOOP))==0
                            && (chan=BASS.BASS_MusicLoad(file, 0, 0, BASS.BASS_SAMPLE_LOOP|BASS.BASS_MUSIC_RAMP, 1))==0) {
                            openbut.setText("Open");
                            playtb.setEnabled(false);
                            playtb.setChecked(false);
                            stopbut.setEnabled(false);
                            applybut.setEnabled(false);
                            cycletb.setEnabled(false);
                            cycletb.setChecked(false);
                            return;
                        }
                        openbut.setText(file);
                        playtb.setEnabled(true);
                        playtb.setChecked(true);
                        stopbut.setEnabled(true);
                        applybut.setEnabled(true);
                        cycletb.setEnabled(true);
                        hrtf=new yvHRTF(chan);
                        ApplyClick(null);
                        hrtf.Start3D();
                        BASS.BASS_ChannelPlay(chan,false);
                    }
                }
            })
               .show();
    }

    public void PlayClick(View v) {
        if (playtb.isChecked()) {
            BASS.BASS_ChannelPlay(chan,false);
            stopbut.setEnabled(true);
        } else {
            BASS.BASS_ChannelPause(chan);
        }
    }

    public void StopClick(View v) {
        BASS.BASS_ChannelStop(chan);
        BASS.BASS_ChannelSetPosition(chan,0,0);
        playtb.setChecked(false);
        stopbut.setEnabled(false);
    }
    
    public void ApplyClick(View v) {
        hrtf.MoveSpeaker(new Vec(Double.parseDouble(xtext.getText().toString()),Double.parseDouble(ytext.getText().toString()),Double.parseDouble(ztext.getText().toString())));
    }
    
    public void CycleClick(View v) {
        cycletb.setChecked(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }
    }

}
