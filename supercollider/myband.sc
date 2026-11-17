// Definiciones propias compatibles con el contrato OSC de Power Band.
// Evaluar todo el bloque en SuperCollider antes de iniciar Java.
(
s.waitForBoot({
    SynthDef(\classicPiano, { |note = 60, durMs = 300, third = 4|
        var frecuencias = [note, note + third, note + 7].midicps;
        var envolvente = EnvGen.kr(
            Env.perc(0.005, (durMs / 1000).max(0.01)), doneAction: 2);
        var senal = Mix(SinOsc.ar(frecuencias) +
            (SinOsc.ar(frecuencias * 2) * 0.2)) * envolvente * 0.08;
        Out.ar(0, senal ! 2);
    }).add;

    SynthDef(\stringBass, { |note = 36, durMs = 300|
        var frecuencia = note.midicps;
        var duracion = (durMs / 1000).max(0.01);
        var envolvente = EnvGen.kr(Env.perc(0.005, duracion), doneAction: 2);
        var senal = Pluck.ar(WhiteNoise.ar(0.3), Impulse.kr(0),
            1, frecuencia.reciprocal, duracion, 0.5);
        Out.ar(0, (senal * envolvente * 0.4) ! 2);
    }).add;

    SynthDef(\organ70s, { |freq = 440, gate = 1,
        r1 = 1, r2 = 2, r3 = 3, a1 = 1, a2 = 0.5, a3 = 0.25|
        var envolvente = EnvGen.kr(
            Env.adsr(0.01, 0.05, 0.9, 0.1), gate, doneAction: 2);
        var senal = Mix(SinOsc.ar(freq * [r1, r2, r3]) * [a1, a2, a3]);
        Out.ar(0, (senal * envolvente * 0.1) ! 2);
    }).add;

    s.sync;
    "Power Band: classicPiano, stringBass y organ70s cargados.".postln;
});
)