# Create: Odds 'n Ends

A Create addon of old workshop machines that do one odd job each: a copy lathe that turns blocks
into shapes, a jacquard loom that weaves pictures from punched cards, and a barrel organ that plays
punched music through pipes, drums and whistles.

For Minecraft 1.21.1 on NeoForge, needs Create 6. JEI shows the lathe, loom and punch recipes, and
every machine has Ponder scenes. Hold W over one in your inventory to see them.

## Copy lathe

Lay three lathe beds in a row with a headstock at one end and a tailstock at the other,
then wrench the headstock to put it together. Give it rotation and it cuts the block in it, one layer
at a time, into the shape on its profile template. The block keeps its material, so an oak log
gives you an oak spindle.

You draw templates on the template table. Drag its 16 bars to set how wide each layer is.

To copy a piece you already turned, build a tracer row two blocks behind the lathe. That's three tracer
rails with a holder over each end. Load the piece into a holder and the lathe follows it instead of
the template.

## Jacquard loom

The loom is a 3×3×2 frame with the jacquard head on top and the loom drive in the bottom front corner.
Wrench the head to put it together, or to find out which spot is wrong. Feed it wool and a punched
card chain, give the drive rotation, and it weaves a tapestry you can hang on a wall or lay down
as a rug. Faster rotation weaves faster.

Chains come from the card punch. Right-click it to draw a design, save, then give it rotation and
blank cards. It punches one card per row.

## Barrel organ

The organ plays book music, which also comes from the card punch. You can write a score there or
import a MIDI file. The organ makes no sound by itself. Run wind lines out from it to wind chests and
put something on each chest to play the notes.

- Steam whistles blow on their own note, no boiler needed. The back of the chest switches between
  exact pitch and any octave.
- The reed pipe, wood pipe and reed box play every note the chest lets through, at real pitch,
  up to 6 at once.
- Kick and snare drums play their own lane of the score, whatever the chest's filter says.

Put a dye in a chest's filter to give it one voice of the score. Rotation turns the pages, faster
plays faster. Redstone pauses it. On *Play once* it gives a short pulse when the book ends, on *Loop*
it starts over.

### Sound and pause screens

In singleplayer, any screen that pauses the game, like Ponder or the Esc menu, also pauses every sound that's
playing, and they pick back up when you close it. Without that, a held organ note or a whistle on a
wind chest kept going the whole time Ponder was open, since paused sounds don't tick and nothing was
there to stop them. It covers every sound in the game. Multiplayer never pauses, so it
doesn't change anything there.

## Building

```
./gradlew build
```

The jar lands in `build/libs`. `./gradlew runClient` starts a dev client.

## License

All rights reserved.
