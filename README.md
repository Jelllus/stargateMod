
Installation information
=======

This template repository can be directly cloned to get you started with a new
mod. Simply create a new repository cloned from this one, by following the
instructions provided by [GitHub](https://docs.github.com/en/repositories/creating-and-managing-repositories/creating-a-repository-from-a-template).

Once you have your clone, simply open the repository in the IDE of your choice. The usual recommendation for an IDE is either IntelliJ IDEA or Eclipse.

If at any point you are missing libraries in your IDE, or you've run into problems you can
run `gradlew --refresh-dependencies` to refresh the local cache. `gradlew clean` to reset everything 
{this does not affect your code} and then start the process again.

Mapping Names:
============
By default, the MDK is configured to use the official mapping names from Mojang for methods and fields 
in the Minecraft codebase. These names are covered by a specific license. All modders should be aware of this
license. For the latest license text, refer to the mapping file itself, or the reference copy here:
https://github.com/NeoForged/NeoForm/blob/main/Mojang.md

Additional Resources: 
==========
Community Documentation: https://docs.neoforged.net/  
NeoForged Discord: https://discord.neoforged.net/

Credits:
==========
Replicator model (`assets/stargate/models/entity/replicator.obj`, `textures/entity/replicator.png`):
"Replicator SG1" by TaurWing (https://sketchfab.com/TaurWing),
https://sketchfab.com/3d-models/replicator-sg1-e8a67b78936d4092b954589c67214396 —
licensed under CC-BY-4.0 (http://creativecommons.org/licenses/by/4.0/). Converted from glTF to OBJ
(split into body and articulated legs, triangles merged into quads).

Staff weapon model (`assets/stargate/models/item/staff_weapon/`, `textures/item/staff_weapon.png`):
"Inspired by Stargate SG-1: Goa'Uld Staff Weapon" by Ska-Ara (https://sketchfab.com/ska-ara),
https://sketchfab.com/3d-models/inspired-by-stargate-sg-1-goa-uld-staff-weapon-973f843c19ca4496b8a38106f2a96414 —
licensed under CC-BY-NC-SA-4.0 (http://creativecommons.org/licenses/by-nc-sa/4.0/). Converted from glTF to OBJ
with `tools/staff_glb2obj.py`, base colour texture downscaled to 1024 px.
