# Changelog

## 0.1.0

First release. Needs Create 6.0.10 or newer on NeoForge 1.21.1. JEI is optional.

### Copy lathe
- lathe headstock, bed and tailstock. wrench it together, give it rotation and it turns any full block into the shape on a profile template
- template table for drawing profiles, 16 layers from foot to top
- tracer rails and holders behind the lathe copy a turned piece you already have, no template needed
- funnels, hoppers and mechanical arms can feed and empty it

### Jacquard loom
- loom frames, loom drive and jacquard head make a 3x3x2 loom that weaves tapestries out of wool
- card punch with a pixel editor for designs up to 3 by 4 blocks, punches them onto blank cards
- the loom pauses when a colour runs out and carries on when you add more
- tapestries hang on walls, floors and ceilings at the size of their design

### Barrel organ
- barrel organ plays book music, once or on loop. redstone pauses it and it pulses when a song ends
- the card punch has a score tab too, a piano roll with MIDI import, and punches book music one card per page
- wind lines and wind chests carry the music out to steam whistles, no boiler needed
- chests can filter by voice, match exact pitch or any octave, and give off redstone so they work as a sequencer

### CNC mill
- cnc frames, cnc drive and pendant make a 4x3x3 mill. wrench the pendant to put it together, it tells you what's missing
- rotation goes into the drive at the far end from the pendant, faster runs faster
- `createoddsnends:cnc` recipe type for modpacks, up to 9 inputs and 4 outputs with chances.
- by hand you open the doors first, then load it or take things out, parts included before they're cut. click an open door to shut them. it pauses while they're open
- sequenced assembly costs one loop of parts and runs faster than create's line, `cnc.assemblyLoopsCharged` and `cnc.assemblyStepTicks` tune it
- automation feeds the hatch on the roof and empties the hatch on the pendant's end, arms, funnels, chutes, hoppers and pipes all work on them
- arms can also load and unload through the front, the doors open for them on their own
- tools a recipe keeps, like an axe, hang in the tool changer while the job runs
- also runs deploying recipes and sequenced assemblies without fluid steps, `cnc.deploying` and `cnc.sequencedAssembly` server config turn those off
- the tool follows the part's shape, flat items and blocks both

### Other
- recipes for everything that can be crafted, the jacquard head and barrel organ go through the mechanical crafter
- JEI categories for turning, card punching, weaving and the CNC, the lathe and CNC pages show the machine working, plus info pages
- Ponder scenes for every machine
- worlds from before the rename (`createcoolstuffs`) keep their blocks and items
