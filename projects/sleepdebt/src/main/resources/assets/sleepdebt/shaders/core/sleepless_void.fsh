#version 150
in vec2 uv;
uniform float Intensity;
uniform float Time;
uniform float Shock;
uniform vec2 Resolution;
out vec4 fragColor;

float hash21(vec2 p) {
    p=fract(p*vec2(123.34,345.45));
    p+=dot(p,p+34.345);
    return fract(p.x*p.y);
}
float noise(vec2 p) {
    vec2 i=floor(p);
    vec2 f=fract(p);
    f=f*f*(3.0-2.0*f);
    return mix(mix(hash21(i),hash21(i+vec2(1,0)),f.x),
               mix(hash21(i+vec2(0,1)),hash21(i+vec2(1,1)),f.x),f.y);
}
float fBm(vec2 p) {
    float v=0.0;
    float scale=.55;
    for(int i=0;i<4;i++){
        v+=scale*noise(p);
        p=p*2.03+vec2(7.7,3.5);
        scale*=.48;
    }
    return v;
}
void main(){
    float p=clamp(Intensity,0.0,1.0);
    vec2 centered=uv-vec2(.5);
    centered.x*=Resolution.x/max(1.0,Resolution.y)*.68;
    float time=Time;
    vec2 flow=uv*vec2(3.1,2.6);
    float shift=fBm(flow*.75+vec2(time*.13,-time*.19));
    vec2 warped=flow+vec2(shift*1.3,-shift*.9);
    float cloud=fBm(warped*1.85+vec2(-time*.15,time*.2));
    float filaments=noise(warped*5.5+time*.07);
    // The eye of the storm contracts; it is not a vanilla radial darkness cone.
    vec2 dragged=centered+vec2((cloud-.5)*.14,(shift-.5)*.13);
    float radial=length(dragged);
    float pulse=.025*sin(time*3.45)+.012*sin(time*8.4);
    float iris=smoothstep(.045+pulse,.23+pulse,radial+cloud*.055);
    // Dissolved shadows invade even the center at full pressure.
    float mixDensity=clamp(.47+.53*iris+.22*(cloud-.4)+.055*filaments,0.0,1.0);
    float absolute=pow(p,1.45);
    float alpha=clamp(absolute*(.68+.32*mixDensity) + iris*absolute*.28,
                      0.0,.998);
    // A strike briefly collapses the last remaining opening.
    alpha=clamp(alpha+Shock*.39*absolute,0.0,.999);
    // Almost pure black, very slight purple-brown at the moving filaments.
    vec3 ink=vec3(.003,.002,.004)+vec3(.010,.004,.012)*(1.0-cloud);
    fragColor=vec4(ink,alpha);
}
