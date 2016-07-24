s.boot;

{

	SynthDef(\g1rainer,
		{
			arg trate,d,b,dur=0,rate=1.2,amp=1.0,gate=0.0,out=0,attack=0.01,sustain=1.0,release=1.0,minamp=0.0;
			var env = Env.asr(attack,sustain,release);
			var gen = EnvGen.kr(env, Changed.kr(gate));
			if(dur == 0,{dur=1.0/(2*trate)});		
			//dur = (10.rand + 1.0) / (2*trate);
			Out.ar(out,
				TGrains.ar(2,
					Impulse.ar(trate), // trigger
					b, // buffer
					(rate ** WhiteNoise.kr(3).round(1)), // rate
					d*BufDur.kr(b), //center
					//d*BufDur.kr(b),
					dur, //duration
					WhiteNoise.kr(0.6),//pan
					0.1*amp, //amp
					2
				)*(gen+minamp);
			);
		}).load;
	
	s.sync;
	
	
	// Boot the server
	
	// Load an audio file
	~buffers = [
		"track1.wav",
		"track2.wav"
	].collect({|x| Buffer.read(s,x) });
	
	s.sync;
	
	~buff1  = ~buffers[0 % ~buffers.size];
	~buff2 =  ~buffers[1 % ~buffers.size];
	
	~b1 = ~buff1.bufnum;
	~b2 = ~buff2.bufnum;

	~minamp = Bus.control(s);
	~minamp.set(0.1);
	
	~energy  = Bus.control(s);
	~energy.set(0.0);
	~energyinc = 0.05;
	~energydec = 0.03;
	~energywait = 0.1;
	~energytick = {
		//~energy.get({|x| ~energy.set(x * (1.0+ ~energyinc)); (x+~energyinc).postln;});
		~energy.get({|x| x = max(0.001,min(30.0,x + ~energyinc));  ~energy.set(x); x.postln;});	
	};
	~energydecay = {
		~energy.get({|x| ~energy.set(max(0.001,x * (1.0 - ~energydec)));});
	};
	~energyrot = Routine({
		loop {
			~energydecay.();
			~energywait.wait;
		};
	}).play;

	// play the synth
	~b1s = Synth(\g1rainer,[\trate,1.0,\dur,2.0,\rate,1,\d,0.0,\b,~b1,\minamp,0.1]);
	~b1d = Bus.control(s);
	~b1s.map(\d,~b1d);
	~b1s.map(\minamp,~minamp);
	~b1d.set(0.0);
	~b1s.map(\release,~energy);
	//~b1s.map(\sustain,~energy);
	~b1s.set(\sustain,1.0);
	~b1s.set(\trate,1.0);
	~b1s.set(\dur,~energy);
	
	
	// play the synth
	~b2s = Synth(\g1rainer,[\rate,1,\dur,2.0,\d,0.0,\b,~b2,\minamp,0.1]);
	~b2s.map(\minamp,~minamp);
	~b2d = Bus.control(s);
	~b2s.map(\d,~b2d);
	~b2d.set(0.0);
	
	~b2s.map(\d,~b1d);
	
	~b1s.set(\gate,1.0.rand + 0.1);
	~b1s.set(\amp,2.6);
	//~b1s.set(\amp,0.0);
	
	//~b1d.set(1.0.rand);
	~b1s.set(\trate,2.0);
 	
	~b2s.set(\gate,1.0.rand + 0.1);
	~b2s.set(\amp,1.0);
	//~b2s.set(\d,1.0.rand);
	~b2d.set(1.0.rand);
	~b2s.set(\trate,3.0);


	~b1s.set(\dur,10.0);
	~b2s.map(\release,~energy);
	
	//~xsynth2 = { Out.kr(~b2d,MouseX.kr()) }.play;
	//~xsynth1 = { Out.kr(~b1d,MouseX.kr()) }.play;
	//~xsynth1.stop;
	//~xsynth2.stop;

	
	~b2d.get({|x| x.postln;});
	
	// Get the mouse outputs
	

	
	
	~mkgranwrap = {|syn|
		{
			|msg|
			var dd = msg[1];
			dd.postln;
			syn.set(\d,dd);
			syn.set(\amp,1.0);
			syn.set(\gate,1.0.rand + 0.1);
		//~amp.set(1.0);
		};
	};
	
	OSCFunc.newMatching(~mkgranwrap.(x),'/multi/1');
	OSCFunc.newMatching(~mkgranwrap.(y),'/multi/2');
	OSCFunc.newMatching(~mkgranwrap.(z),'/multi/3');
	OSCFunc.newMatching(~mkgranwrap.(u),'/multi/4');
}.fork;

~mkgui = {
	var offset = 0.01, setter, w, c, a, b, offslider, scratch, energy, erot,
	width=1000,height=700, minslider;
	w = Window.new(name:"Energy-Skip",bounds:Rect(0,0,width, height)).front;
	c = NumberBox(w, Rect(20, 20, width/3, 40));
	c.scroll_step_(offset);
	c.step_(offset);
	c.align_(\right);
	energy = NumberBox(w, Rect(width/2, 20, width/3, 40));
	energy.align_(\right);
	erot = Routine({
		loop {
			~energy.get({|x|
				AppClock.sched(0.0, { arg time; 
					energy.value_(x);
					nil
				});
			});
			0.5.wait;
		}
	}).play;
	// this sets the time
	a = Slider(w, Rect(20, 60, 4*width/9, 60))
	.action_({
		setter.(a.value);
	});
	c.action({
		setter.(c.value);
	});
	offslider = Slider(w, Rect(width/2, 60, 4*width/9, 60));
	offslider.action_({
		offset = offslider.value * 0.01;
		c.step_(offset);
		c.scroll_step_(offset);
	});
	minslider = Slider(w, Rect(width-40,0,40,120));
	minslider.value_(0.1);
	minslider.orientation_(\vertical);
	minslider.action_({~minamp.set(minslider.value)});
	// sets the values
	setter = {|val|
		~energytick.();
		~b1d.get({|x|
			var r,out;
			["x",x].postln;
			if (val >= 0.0, { out = val; }, {out = x + offset});
			["out",out].postln;
			AppClock.sched(0.0, { arg time; 
				c.value_(out);
				a.value_(out);
				nil
			});
				~b1d.set(out);
			~b1s.set(\gate,1.0.rand);
			~b2s.set(\gate,1.0.rand);
		});	
	};
	scratch = Slider(w, Rect(20, 180, width-40, height-180-20));
	scratch.action_({
		setter.(-1.0);
		//~b1s.set(\gate,1.0.rand);
		//	~b2s.set(\gate,1.0.rand);		
	});
	b = Button(w, Rect(20, 120, width-40, 60))
	.states_([
		["there is suffering", Color.black, Color.red],
		["the origin of suffering", Color.white, Color.black],
		["the cessation of suffering", Color.red, Color.white],
		["there is a path to cessation of suffering", Color.blue, Color.clear]
	])
	.action_({ arg butt;
			setter.(-1.0);
	});
};
{
	s.sync;
	AppClock.sched(0.0, { arg time;
		~mkgui.();
		nil
	});
}.fork;
