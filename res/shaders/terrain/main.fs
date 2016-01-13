
// Disabled int textures for Intel IGP compatibility, instead it's floats in GL_NEAREST interpolation
//#extension GL_EXT_gpu_shader4 : require

varying vec3 vertex;
varying vec4 color;

varying float fogI;
uniform sampler2D normalTexture;
uniform vec3 sunPos; // Sun position
uniform vec3 camPos;
uniform float time;
varying vec3 eye;
uniform samplerCube skybox;
varying float fresnelTerm;

uniform float waterLevel;
uniform sampler2D heightMap;
uniform sampler2D groundTexture;
//uniform isampler2D groundTexture;
uniform sampler1D blocksTexturesSummary;
varying vec2 textureCoord;

varying float chunkFade;

varying vec2 lightMapCoords;

uniform float sunIntensity;
varying vec3 normalHeightmap;
uniform sampler2D lightColors; // Sampler to lightmap

varying float lowerFactor;

uniform vec3 vegetationColor;

const vec3 shadowColor = vec3(0.20, 0.20, 0.31);
const float shadowStrength = 0.75;

uniform mat4 projectionMatrix;
uniform mat4 projectionMatrixInv;

uniform mat4 modelViewMatrix;
uniform mat4 modelViewMatrixInv;

uniform mat3 normalMatrix;
uniform mat3 normalMatrixInv;

uniform float shadowVisiblity;

uniform sampler2D glowSampler;
uniform sampler2D colorSampler;

<include ../sky/sky.glsl>

void main()
{
	//if(chunkFade < 0.5)
	//	discard;
	
	float id = texture2D(groundTexture, textureCoord).r;
	vec3 finalColor = vec3(0.0+id*500, 0.0, 0.0);
	
	vec4 bs = texture1D(blocksTexturesSummary, id/512.0);
	finalColor = bs.rgb;
	if(bs.a < 1)
		finalColor *= vegetationColor;
	
	float spec = 0.0;
	
	vec3 normal = normalHeightmap;
	<ifdef hqTerrain>
	float baseHeight = texture2D(heightMap,textureCoord).r;
	
	//Normal computation, brace yourselves
	vec3 normalHeightmap2 = vec3(0.0, 1.0, 0.0); //Start with an empty vector
	
	float xtrem = 2;
	
	float normalXplusHeight = texture2D(heightMap,textureCoord+vec2(1.0/256.0, 0.0)).r;
	float alpha = atan((normalXplusHeight-baseHeight)*xtrem);
	vec3 normalXplus = vec3(0.0, cos(alpha), -sin(alpha));
	
	normalHeightmap2 += normalXplus;
	
	float normalZplusHeight = texture2D(heightMap,textureCoord+vec2(0.0, 1.0/256.0)).r;
	alpha = atan((normalZplusHeight-baseHeight)*xtrem);
	vec3 normalZplus = vec3(-sin(alpha), cos(alpha), 0.0);
	
	normalHeightmap2 += normalZplus;
	
	float normalXminusHeight = texture2D(heightMap,textureCoord-vec2(1.0/256.0, 0.0)).r;
	alpha = atan((normalXminusHeight-baseHeight)*xtrem);
	vec3 normalXminus = vec3(0.0, cos(alpha), sin(alpha));
	
	normalHeightmap2 += normalXminus;
	
	float normalZminusHeight = texture2D(heightMap,textureCoord-vec2(0.0, 1.0/256.0)).r;
	alpha = atan((normalZminusHeight-baseHeight)*xtrem);
	vec3 normalZminus = vec3(sin(alpha), cos(alpha), 0.0);
	
	normalHeightmap2 += normalZminus;
	
	//I'm happy to say I came up with the maths by myself :)
	
	normal = normalize(normalHeightmap2);
	<endif hqTerrain>
	
	float specular = 0.0;
	
	if(id == 128)
	{
		
		vec3 nt = 1.0*(texture2D(normalTexture,(vertex.xz/5.0+vec2(0.0,time)/50.0)/15.0).rgb*2.0-1.0);
		nt += 1.0*(texture2D(normalTexture,(vertex.xz/2.0+vec2(-time,-2.0*time)/150.0)/2.0).rgb*2.0-1.0);
		nt += 0.5*(texture2D(normalTexture,(vertex.zx*0.8+vec2(400.0, sin(-time/5.0)+time/25.0)/350.0)/10.0).rgb*2.0-1.0);
		nt += 0.25*(texture2D(normalTexture,(vertex.zx*0.1+vec2(400.0, sin(-time/5.0)-time/25.0)/250.0)/15.0).rgb*2.0-1.0);
		
		nt = normalize(nt);
		
		float i = 0.25;
		
		normal.x += nt.r*i;
		normal.z += nt.g*i;
		normal.y += nt.b*i;
		
		normal = normalize(normal);
		
		//specular = max(pow(dot(normalize(reflect(normalMatrix * eye,normalMatrix * normal)),normalize(normalMatrix * sunPos)),150.0),0.0);
		
		spec = fresnelTerm;
		
		specular = spec * pow(clamp(dot(normalize(reflect(normalMatrix * eye,normalMatrix * normal)),normalize(normalMatrix * sunPos)), 0.0, 1.0),1750.0);
	
		//vec3 reflection = texture(skybox, reflect(eye, normal)).rgb;
		
	}

	vec3 blockLight = texture2D(lightColors,vec2(lightMapCoords.x, 0)).rgb;
	vec3 sunLight = texture2D(lightColors,vec2(0, lightMapCoords.y)).rgb;
	
	float opacity = 0.0;
	float NdotL = clamp(dot(normal, normalize(sunPos)), -1.0, 1.0);
	
	//opacity += NdotL;
	
	float clamped = clamp(NdotL, 0.0, 0.1);
	if(NdotL < 0.1)
	{
		opacity += 1-(10*clamped);
	}
	
	opacity = clamp(opacity, 0, 0.52);
	
	sunLight = mix(sunLight * shadowColor, sunLight, (1-opacity) * shadowVisiblity);
	
	vec3 finalLight = blockLight;// * (1-sunLight);
	finalLight += sunLight;
	
	//finalLight = mix(finalLight, finalLight*shadowColor, opacity * 1.0);
	//finalColor*=finalLight;
	
	finalColor = finalColor * finalLight + vec3(10.0) * specular;
	
	if(spec >0)
		finalColor = mix(finalColor, getSkyColor(time, normalize(reflect(eye, normal))), spec);
	
	
	//finalColor = vec3(1.0);
	gl_FragColor = mix(vec4(finalColor, 1.0),vec4(gl_Fog.color.rgb,1.0),1.0-fogI);
}