package org.cs3.timetracker;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.Timer;

/*
 * TimeTicker is a class, which provides a count down clock.
 * Interface is
 * 	(start()
 * stop()
 * 
 * resue()
 */

public class TimeTicker  {
	private Timer t;
	private TimeEvent time;
	private ArrayList observers;
	
	public String Log = ""; 
	
	private int Seconds = 180; // 3* 60
	
	public void addObserver(ITimeObserver observer)
	{
		observers.add(observer);	
	}
	
	public void TimeTick()
	{
		time.setMinutes(Seconds / 60);
  		time.setSeconds(Seconds - ((Seconds / 60) * 60));
  		Seconds--;

  		if (Seconds == 0) {
  			stop();
  		}

  		Iterator i = observers.iterator();
  		while (i.hasNext()) {
  			ITimeObserver a = (ITimeObserver) i.next();
  			a.notify(time);
  		}
	}

	public TimeTicker()
	{
		observers = new ArrayList();
		time = new TimeEvent(0,0);
		ActionListener action = new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				TimeTick();
			}
		};
				
		t = new Timer(0, action);
		t.setRepeats(false);
	}
	
	public void start()
	{
		Seconds = 180;
		t.setRepeats(true);
		t.setDelay(1000);
		t.start();
		
		Log = Log + "Started.\n";
	}
	
	public void stop()
	{
		t.setRepeats(false);
		t.stop();
		Seconds = 0;
		Log = Log + "Stopped.\n";
	}
	
	public void pause()
	{
		t.stop();
		Log = Log + "Paused.\n";
	}
	
	public void resume()
	{
		t.start();
		Log = Log + "Resumed.\n";
	}

}
