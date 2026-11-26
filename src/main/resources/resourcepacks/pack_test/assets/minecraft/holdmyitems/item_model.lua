global.yawAngle = 0;
global.yawAngleO = 0;
global.pitchAngle = 0;
global.pitchAngleO = 0;
global.fall = 0;
global.a = 0;
global.walk = 0;
global.walkSmoother = 0;
global.fall_f = 0;
global.jiggle_f = 0;
global.mainHandSwitch = 0;
global.offHandSwitch = 0;
global.jiggle_i = 0.0;

global.axolotl_anim = 1;
global.pufferfish_anim = 1;
global.salmon_anim = 1;
global.cod_anim = 1;
global.tadpole_anim = 1;
global.sulfur_cube_anim = 1;
global.liquid_anim = 1;
global.tropical_fish_anim = 1;
global.chest_boat_anim = 1;

local l = data.bl and 1 or -1
jiggle_i = jiggle_i + P:getYSpeed(data.player) * data.deltaTime * 30
if not P:isOnGround(data.player) and P:getYSpeed(data.player) * -1 > 0.55 then
    fall_f = fall_f + 0.045 * data.deltaTime * 30;
else
    fall_f = fall_f - 0.07 * data.deltaTime * 30
end
fall_f = M:clamp(fall_f, 0, 1)
local wf = M:clamp(fall * 0.4, 0, 2) * (fall_f * fall_f * fall_f)
local l = data.bl and 1 or -1

local ywAngle = data.mainHand and yawAngle or yawAngleO
local ptAngle = data.mainHand and pitchAngle or pitchAngleO
if I:isOf(data.item, Items:get("minecraft:water_bucket")) or I:isOf(data.item, Items:get("minecraft:lava_bucket")) or I:isOf(data.item, Items:get("minecraft:milk_bucket")) or I:isOf(data.item, Items:get("minecraft:sulfur_cube_bucket")) then
    animator:rotateX(0, 1, M:clamp(-ptAngle * 0.6, -5, 5) * liquid_anim, 0.5, 0.7, 0.5)
    animator:rotateZ(0, 1, M:clamp(ywAngle * 0.6, -5, 5) * liquid_anim, 0.5, 0.7, 0.5)
end

if I:isOf(data.item, Items:get("minecraft:sulfur_cube_bucket")) then
    local sulfurSquish = 0.04 * math.sin(a * 2.4) * sulfur_cube_anim
    animator:moveY(36, 47, fall * 0.06 * sulfur_cube_anim)
    animator:moveY(36, 47, 0.04 * math.sin(a * -1.5) * (1-walkSmoother) * sulfur_cube_anim)
    animator:moveY(36, 47, 0.08 * Easings:easeInSine(math.abs(math.sin(walk))) * walkSmoother * sulfur_cube_anim)
    animator:scale(36, 47, 1 - sulfurSquish, 1 + sulfurSquish, 1 - sulfurSquish)
end

if I:isOf(data.item, Items:get("minecraft:axolotl_bucket")) then

    animator:moveY(0, 11, fall * 0.02 * axolotl_anim)
    animator:moveY(0, 11, 0.02 * math.sin(a * -1.5)  * (1-walkSmoother) * axolotl_anim)
    animator:moveY(0, 11,  0.08 * Easings:easeInSine(math.abs(math.sin(walk))) * walkSmoother * axolotl_anim)
    animator:rotateX(0, 11, M:clamp(20 * fall, -20, 20) * axolotl_anim, 0.5, 0.7, 0.5)
    animator:rotateZ(0, 11, 2 * math.sin(a) * axolotl_anim, 0.5, 0.7, 0.5)
    animator:rotateX(0, 11, ptAngle * 0.3 * axolotl_anim, 0.5, 0.7, 0.5)
    animator:rotateZ(0, 11, -ywAngle * 0.3  * axolotl_anim, 0.5, 0.7, 0.5)

    animator:moveY(12, 19, 0.02 * math.sin(a * -1.5) * (1-walkSmoother) * axolotl_anim)
    animator:moveY(12, 19, 0.08 * Easings:easeInSine(math.abs(math.sin(walk))) * walkSmoother * axolotl_anim)
    animator:rotateX(12, 19, M:clamp(15 * fall, -20, 20) * axolotl_anim, 0.5, 0.7, 0.5)
    animator:rotateZ(12, 19, 1 * math.sin(a) * axolotl_anim, 0.5, 0.7, 0.5)
    animator:rotateX(12, 19, ptAngle * 0.15 * axolotl_anim, 0.5, 0.7, 0.5)
    animator:rotateZ(12, 19, -ywAngle * 0.15  * axolotl_anim, 0.5, 0.7, 0.5)

    animator:rotateY(8, 9, 10 * math.sin(a * 10) * M:pow(math.sin(a * 0.4), 30) * axolotl_anim, 0.8, 0.5, 0.4)
    animator:rotateY(10, 11, -10 * math.sin(a * 10) * M:pow(math.sin(a * 0.4), 30) * axolotl_anim, 0.3, 0.5, 0.4)
    animator:rotateX(20, 21, 80 * wf * axolotl_anim, 0.5, 0.85, 0.3)
    animator:rotateZ(20, 21, 15 * wf * axolotl_anim, 0.5, 0.85, 0.3)
    animator:rotateX(22, 23, 80 * wf * axolotl_anim, 0.5, 0.85, 0.3)
    animator:rotateZ(22, 23, -15 * wf * axolotl_anim, 0.5, 0.85, 0.3)

    animator:moveX(0, 23, fall * 0.02 * axolotl_anim)
    animator:moveZ(0, 23, fall * 0.025 * axolotl_anim)
    animator:scale(0, 23,  1 - (0.03 * fall) * axolotl_anim, 1 + (0.03 * fall) * axolotl_anim, 1 - (0.03 * fall) * axolotl_anim)

end

if I:isOf(data.item, Items:get("minecraft:tropical_fish_bucket")) then

    animator:moveY(0, 12, fall * 0.1 * tropical_fish_anim)
    animator:moveY(0, 12, 0.02 * math.sin(a * -1.5)  * (1-walkSmoother) * tropical_fish_anim)
    animator:moveY(0, 12, 0.08 * Easings:easeInSine(math.abs(math.sin(walk))) * walkSmoother * tropical_fish_anim)
    animator:moveZ(0, 12, 0.1 * tropical_fish_anim)
    animator:moveY(0, 12, -0.03 * tropical_fish_anim)
    animator:moveY(0, 12, -ywAngle * 0.002 * tropical_fish_anim)
    animator:rotateX(0, 12, -ywAngle * tropical_fish_anim , 0.5, 0.5, 0.5)
    animator:rotateZ(0, 12, -ptAngle * tropical_fish_anim , 0.5, 0.5 , 0.5)
    animator:rotateX(0, 12, 3 * math.sin(a * 3)  * (1-walkSmoother) * tropical_fish_anim, 0.5, 0.5, 0.5)

    animator:rotateZ(6, 9, 15 * math.sin(a * 3) * tropical_fish_anim, 0.5, 0.5 , 0.5)
end

if I:isOf(data.item, Items:get("minecraft:cod_bucket")) then

    animator:scale(0, 25, 1 - 0.1 * cod_anim, 1 - 0.1 * cod_anim, 1 - 0.1  * cod_anim)
    animator:moveY(0, 25, fall * 0.1 * cod_anim)
    animator:moveY(0, 25, 0.05 * cod_anim)
    animator:moveY(0, 25, 0.02 * math.sin(a * -1.5)  * (1-walkSmoother) * cod_anim)
    animator:moveY(0, 25, 0.08 * Easings:easeInSine(math.abs(math.sin(walk))) * walkSmoother * cod_anim)
    animator:moveZ(0, 25, 0.1 * cod_anim)
    animator:moveY(0, 25, -0.03 * cod_anim)
    animator:moveY(0, 25, (-ywAngle * 0.002)  * cod_anim)
    animator:rotateX(0, 25, -ywAngle * 0.6 * cod_anim , 0.5, 0.5, 0.5)
    animator:rotateZ(0, 25, -ptAngle * 0.6 * cod_anim  , 0.5, 0.5, 0.5)
    animator:rotateX(0, 25, 3 * math.sin(a * 1.5)  * (1-walkSmoother) * cod_anim, 0.5, 0.5, 0.5)
    animator:rotateX(0, 25, fall * -10 * cod_anim, 0.5, 0.5, 0.5)

    animator:moveX(10,21, -0.05 * l * cod_anim)
    animator:rotateX(10,21, -ywAngle * 0.2 * cod_anim, 0, 0.6, 0.2)
    animator:rotateX(10,21, fall * -5 * cod_anim, 0, 0.6, 0.2)
end
if I:isOf(data.item, Items:get("minecraft:salmon_bucket")) then
    animator:scale(0, 52, 1 - 0.15 * salmon_anim, 1 - 0.15 * salmon_anim, 1 - 0.15 * salmon_anim)
    animator:moveY(0, 52, fall * 0.1 * salmon_anim)
    animator:moveY(0, 52, 0.04 * salmon_anim)
    animator:moveY(0, 52, 0.02 * math.sin(a * -1.5)  * (1-walkSmoother) * salmon_anim)
    animator:moveY(0, 52, 0.08 * Easings:easeInSine(math.abs(math.sin(walk))) * walkSmoother * salmon_anim)
    animator:moveZ(0, 52, 0.1 * salmon_anim)
    animator:moveY(0, 52, (-ywAngle * 0.002) * salmon_anim )
    animator:rotateX(0, 52, -ywAngle * 0.6 * salmon_anim  , 1, 0.5, 0.5)
    animator:rotateZ(0, 52, -ptAngle * 0.6 * salmon_anim  , 0.5, 0.5, 0.5)
    animator:rotateX(0, 52, 3 * math.sin(a * 1.5)  * (1-walkSmoother) * salmon_anim , 0.5, 0.5, 0.5)
    animator:rotateX(0, 52, fall * -10 * salmon_anim , 0.5, 0.5, 0.5)

    animator:rotateX(0, 5, -ywAngle * 0.2 * salmon_anim , 0, 0.6, 0.25)
    animator:rotateX(0, 5, fall * -5 * salmon_anim , 0, 0.6, 0.25)
end

if I:isOf(data.item, Items:get("minecraft:tadpole_bucket")) then
    animator:moveY(0, 7, fall * 0.1 * tadpole_anim)
    animator:moveY(0, 7, 0.02 * math.sin(a * -1.5)  * (1-walkSmoother) * tadpole_anim)
    animator:moveY(0, 7, 0.08 * Easings:easeInSine(math.abs(math.sin(walk))) * walkSmoother * tadpole_anim)
    animator:moveZ(0, 7, 0.1 * tadpole_anim)
    animator:moveY(0, 7, -0.03 * tadpole_anim)
    animator:moveY(0, 7, (-ywAngle * 0.002)  * tadpole_anim)
    animator:rotateY(0, 7, -ywAngle * 0.6 * tadpole_anim  , 0.5, 0.5, 0.5)
    animator:rotateX(0, 7, -ptAngle * 0.6 * tadpole_anim , 0.5, 0.5, 0.5)
    animator:rotateZ(0, 7, -ywAngle * 0.6 * tadpole_anim  , 0.5, 0.5, 0.5)
    animator:rotateX(0, 7, 3 * math.sin(a * 3)  * (1-walkSmoother) * tadpole_anim, 0.5, 0.5, 0.5)
end

if I:isOf(data.item, Items:get("minecraft:pufferfish_bucket")) then

    animator:moveY(0, 29, fall * 0.1 * pufferfish_anim)
    animator:moveY(0, 29, 0.02 * math.sin(a * -1.5)  * (1-walkSmoother) * pufferfish_anim)
    animator:moveY(0, 29, 0.08 * Easings:easeInSine(math.abs(math.sin(walk))) * walkSmoother * pufferfish_anim)
    animator:moveZ(0, 29, 0.1 * pufferfish_anim)
    animator:moveY(0, 29, -0.03 * pufferfish_anim)
    animator:moveY(0, 29, (-ywAngle * 0.002)  * pufferfish_anim)
    animator:rotateX(0, 29, M:abs(-ywAngle) * 0.2 * pufferfish_anim, 0.5, 0.5, 0.5)
    animator:rotateX(0, 29, -ptAngle * 0.2 * pufferfish_anim, 0.5, 0.5, 0.5)
    animator:rotateZ(0, 29, -ywAngle * 0.2 * pufferfish_anim, 1, 0.5, 0.5)
    animator:rotateX(0, 29, 1 * math.sin(a * 3)  * (1-walkSmoother) * pufferfish_anim, 0.5, 0.5, 0.5)

    animator:rotateZ(6, 7, 10 * math.sin(a * 10) * M:pow(math.sin(a * 0.4), 30) * pufferfish_anim, 0.5, 0.5, 0.5)
    animator:rotateZ(8, 9, -10 * math.sin(a * 10) * M:pow(math.sin(a * 0.4), 30) * pufferfish_anim, 0.5, 0.5, 0.5)
end

if I:isIn(data.item, Tags:getVanillaTag("chest_boats")) then
    animator:moveY(0, 15, M:clamp(fall * 0.1, 0, 1) * chest_boat_anim)

    animator:rotateX(0, 11, M:clamp(ptAngle * 1.35, 0, 999) * chest_boat_anim , 0.5, 0.5, 0.8)

    animator:rotateZ(0, 15, -ywAngle * 0.12 * chest_boat_anim, 0.5, 0, 0.5)

end

