7s.boot;

//
~guigo = {|f|
	AppClock.sched(0.0, { arg time;
		f.();
		nil
	});
};


{
	SynthDef(\g1rainer,
		{
			arg trate=1.0,d=0.0,b,dur=0,rate=1.2,amp=1.0,gate=0.0,out=0,attack=0.01,sustain=1.0,release=1.0,minamp=0.0;
			var env = Env.asr(attack,sustain,release);
			var gen = EnvGen.kr(env, Changed.kr(gate));
			if(dur == 0,{dur=1.0/(2*trate)});		
			//dur = (10.rand + 1.0) / (2*trate);
			Out.ar(out,
				TGrains.ar(2,
					Impulse.ar(trate), // trigger
					b, // buffer
					rate,//
					//(rate ** WhiteNoise.kr(3).round(1)), // rate
					d*BufDur.kr(b), //center
					//d*BufDur.kr(b),
					dur, //duration
					0,//WhiteNoise.kr(0.6),//pan
					0.1*amp, //amp
					2
				)*(gen+minamp);
			);
		}).load;
	// rate 0 to 1 -2 to 2
	SynthDef(\PlayBuf, {| out = 0, bufnum = 0, rate=0.5, amp=1.0, lp=1.0,hp=0.0 |
		Out.ar(out,
			LPF.ar(
				HPF.ar(
					PlayBuf.ar(1, bufnum, BufRateScale.kr(bufnum), rate: 1.0 * (1.0 + rate - 0.5), loop: 1.0),
					freq: 20 + 4980*hp
				),				
				freq: 5000*lp,
				mul: amp
			)!2
		)
	}).load;


	
	//sync
	s.sync;
	// Boot the server
	// Load an audio file
	~buffers = [
		"track1.wav",
		"track2.wav",
		"track3.wav",
		"don-747.wav"
	].collect({|x| Buffer.read(s,x) });
	~don = Buffer.read(s,"don-747.wav");
	~dons =	Synth.newPaused(\PlayBuf, [\out, 0, \bufnum, ~don.bufnum]);
	//~dons.run;
	//Synth(\g1rainer,[\trate,1/(60*16),\dur,60.0*16,\rate,1,\d,0.0,\b,~don.bufnum,\minamp,0.5,\amp,10.0]);

	// sync
	s.sync;
	// buffers
	~buff1  = ~buffers[0 % ~buffers.size];
	~buff2 =  ~buffers[1 % ~buffers.size];
	~buff3 =  ~buffers[2 % ~buffers.size];
	~buff4 =  ~buffers[3 % ~buffers.size];

	// buff nums
	~b1 = ~buff1.bufnum;
	~b2 = ~buff2.bufnum;
	~b3 = ~buff3.bufnum;
	~b4 = ~buff4.bufnum;
	// minimum amplitude bus (the quietest)
	~minamp = Bus.control(s);
	~minamp.set(0.1);
	// Energy stuff
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
	// play b1
	// play the synth
	~b1s = Synth(\g1rainer,[\trate,1.0,\dur,2.0,\rate,1,\d,0.0,\b,~b1,\minamp,0.1]);
	~b1d = Bus.control(s);
	~b1s.map(\d,~b1d);
	~b1s.map(\minamp,~minamp);
	~b1d.set(0.0);
	//~b1s.map(\sustain,~energy);
	~b1s.set(\sustain,1.0);
	~b1s.set(\trate,1.0);
	// Play b2
	// play the synth
	~b2s = Synth(\g1rainer,[\rate,1,\dur,2.0,\d,0.0,\b,~b2,\minamp,0.1]);
	~b2s.map(\minamp,~minamp);
	~b2d = Bus.control(s);
	~b2s.map(\d,~b2d);
	~b2d.set(0.0);	
	~b2s.map(\d,~b1d);
	// play b3
	// play the synth
	~b3s = Synth(\g1rainer,[\rate,1,\dur,2.0,\d,0.0,\b,~b3,\minamp,0.1]);
	~b3s.map(\minamp,~minamp);
	~b3d = Bus.control(s);
	~b3s.map(\d,~b3d);
	~b3d.set(0.0);	
	~b3s.map(\d,~b1d);
	// play b4
	// play the synth
	~b4s = Synth(\g1rainer,[\rate,1,\dur,2.0,\d,0.0,\b,~b4,\minamp,0.1]);
	~b4s.map(\minamp,~minamp);
	~b4d = Bus.control(s);
	~b4s.map(\d,~b4d);
	~b4d.set(0.0);	
	~b4s.map(\d,~b1d); 	
	// now map the busses
	~bs = [ ~b1s, ~b2s, ~b3s, ~b4s ];
	~bs.do {|bs|
		bs.map(\dur,~energy);
		bs.map(\amp,~energy);
		bs.map(\release,~energy);
		bs.map(\release,~trate);
	};
	//~xsynth2 = { Out.kr(~b2d,MouseX.kr()) }.play;
	//~xsynth1 = { Out.kr(~b1d,MouseX.kr()) }.play;
	//~xsynth1.stop;
	//~xsynth2.stop;

~mkgui = {
	var offset = 0.01, setter, w, c, a, b, offslider, scratch, energy, erot,
	width=1000,height=700, minslider, startbutton;
	w = Window.new(name:"Energy-Skip",bounds:Rect(0,0,width, height)).front;
	startbutton = Button(w, Rect(20,0,width/10,20));
	startbutton.action_({~dons.run;});
	startbutton.states_([
		["Start", Color.black, Color.red],
		["Playing", Color.white, Color.black]]);
	c = NumberBox(w, Rect(20, 20, width/3, 40));
	c.scroll_step_(offset);
	c.step_(offset);
	c.align_(\right);
	c.minDecimals_(5);
	energy = NumberBox(w, Rect(width/2, 20, width/3, 40));
	energy.align_(\right);
	erot = Routine({
		loop {
			~energy.get({|x|
				~guigo.({
					energy.value_(x);						
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
			~guigo.(
				{
					c.value_(out);
					a.value_(out);
				}
			);
			~bs.do{|bs| bs.set(\date,1.0.rand); };
			~b1d.set(out);
		});	
	};
	scratch = Slider(w, Rect(20, 180, width-40, height-180-20));
	scratch.action_({
		setter.(-1.0);
	});
	scratch.keyDownAction_({
		setter.(-1.0);
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
	s.sync;
	~guigo.(~mkgui);
}.fork;
