# Ultimates Card Creation
This tutorial covers the basics on creating and registering a card for Ultimates. Cards can be created quickly using the 
abstraction layer, but the real challenge lies in card integration.

## A note about wide-support
This plugin is design to run as or in conjunction with open-world gamemodes. Examples: Factions, HCF, KotH, Prison, 
SkyBlock, Skygrid, etc. As such, cards often need to implement either general or custom Listeners to give other plugins
a chance to cancel the outcomes of a card. More on this later, but keep this in mind during development.

## Creating your first card
Let's use the `WormCard` as our example to learn from. Here is the `WormCard` class code:
```java
@CardInfo(
        material = Material.LEAD,
        name = "worms-burrow",
        display = "§7Worm's Burrow", // Typically we want the color to match the Primal
        description = {"§7You can §ebreak §7dirt", "§7blocks §cinstantly."},
        source = PrimalSource.MOON,
        tier = Tier.RARE
)
public class WormCard extends Card {

    public WormCard(Ultimates plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDirtClick(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if(drawn.contains(p) && e.getHand() == EquipmentSlot.HAND && e.getAction() == Action.LEFT_CLICK_BLOCK
                && (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE)) {
            Block clicked = e.getClickedBlock();
            if(clicked.getType() == Material.DIRT) {
                BlockBreakEvent blockBreak = new BlockBreakEvent(clicked, p);
                Bukkit.getPluginManager().callEvent(blockBreak);
                if(!blockBreak.isCancelled()) {
                    e.setCancelled(true);
                    clicked.breakNaturally();
                }
            }
        }
    }
}
```

Let's break this down. First and foremost, each card needs it's own class and naming scheme. The conventions used for
these class names are:
1. Choose a name centered around terms for what you're trying to build.
2. Capitalize the first letter of each word in the class name.
3. End the class name with `Card`.

For example `HelloWorldCard` is acceptable, but `Helloword` is not.

Next up, each card needs to extend the `Card` class. This will give you access to some important functionality we'll
discuss later.

Then, you'll need to register your card to the `Cards` class so that Ultimates knows about it when it starts up. Find
the `cards` collection in this class at the top. At the very end of this list, add your class in with `ExampleCard.class`.
```java
private static final List<Class<? extends Card>> cards = Arrays.asList(
        OOCRegenerationCard.class, CultivatorCard.class, WormCard.class, RubberSkinCard.class,
        ForceLevitationCard.class, StrangeBowCard.class, VeganCard.class, SuplexCard.class,
        RubberProjectileCard.class, ZeroGravityProjectileCard.class, DeflectionCard.class, MagmaWalkerCard.class,
        SplashPotionOfGetHisAssCard.class);
```

Once that's done, each card needs a `@CardInfo` annotation just above the class declaration. Each card must supply the
following information.
1. A material. This is simply the type of item to use for the icon of your card. Choose something fitting. If you need 
to modify metadata on the item for Potions and stuff, see `cacheItemStacks` in `SplashPotionOfGetHisAssCard` for an example.
2. The technical name of the card. Use the name of the class, or the display name here, with some changes:
make the name all lowercase and separate words using hyphens.
3. The display name of the card. Choose a fun name, nothing too technical. Preface the name with a color code matching
the `PrimalSource` of your choosing's color. This isn't *required*, but just a coloring scheme we're following for
clarity.
4. Type out the description of the card. Use `§7` as the default chat color. Use other chat colors to highlight keywords
in your card's description. Each segment in the array appears on it's own line. Keep each line roughly 16 characters or 
so in length not including chat color. This is because some people have super small computers and won't see the entire
lore of an item when they hover over it.
5. Give your card a fitting `PrimalSource`. Remember to match the card display name chat color with whatever you pick.
PrimalSources should be spread out. `Earth` is way too easy to pick for every card. So be creative.
6. Choose a Tier for your card. Common cards should be `PrimalSource.NONE`. Legendary tier'd cards will only be given to
one player at a time. Elusive cards will be given out once to a group of people, then never again available. Other than
that, pick a Tier that matches how much time was spent on the card, and how cool the card is.

Once your annotation is built, add a default constructor similar to the `WormCard` above, but with your class name. You
shouldn't need to modify this except for overriding the default card constructor or adding variables to initialize once
for your card.

You're all set! The card is ready to be worked on. The EventListener registration is handled for you, so just start
making events related to your card.

## Compatibility with other plugins
It is highly recommended to ignore cancellations and add priorities with the annotation: 
`@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)` 

The EventPriority should usually be either `HIGH` or `HIGHEST` for cards. All of this is for compatibility with other 
plugins. Additionally, you may need to create, fire, and analyze events relating to your card to ensure it can be
cancelled by other plugins. Here's an example from the `MagmaWalkerCard` that creates magma blocks for players who sneak
over lava. Obviously, we don't want players to destroy our spawns with this card. So we need to give plugins like 
WorldGuard a chance to cancel the effects of the card. We'll do this by calling a `BlockPlaceEvent` native to Spigot.
```java
BlockPlaceEvent bpe = new BlockPlaceEvent(below, below.getState(),
        below.getWorld().getBlockAt(below.getLocation().clone().subtract(0, 1, 0)),
                new ItemStack(Material.MAGMA_BLOCK), p, true, EquipmentSlot.HAND);
plugin.getServer().getPluginManager().callEvent(bpe);
if(!bpe.isCancelled()) {
    below.setType(Material.MAGMA_BLOCK);
} else
    p.sendMessage("§cYou cannot use your " + info.display() + "§c card here!");
```

First, a BlockPlaceEvent is generated using the provided Spigot constructor. We pass information we know about to the
event so that it can pass that info to other plugins. Next, we call the event using the `PluginManager`. At this point,
every plugin (including ours) gets a say in what should happen to the event. At the end of this call, our 
`BlockPlaceEvent` instance has probably been modified by other plugins. So going forward, we make calls to the event to
retrieve our modified data. The most important thing to pay attention to is `bpe.isCancelled()`. This returns true if
our event got cancelled by a plugin. If this happened, we **should not** let the effects of the card occur.

You can also create and call custom events relating to cards if you feel they are needed. See the `events` package for
examples of custom events. They are called and executed in the same was as the default, built-in Spigot events. But keep
in mind, the event is custom, so the majority of plugins on the market won't be implementing handlers for your custom
event.

## Helpful tips
Here's a few things to keep in mind while making cards.
1. Only one instance of each card is created per server start.
2. A list of players with a card drawn can be accessed with `drawn.contains(Player)`.
3. Players will draw and discard their cards upon joining and leaving.
4. If you need to do something when a card is drawn or discarded, override those methods. Example from the 
`ForceLevitationCard`: 
```java
@Override
public boolean draw(Player p) {
    boolean didEquip = super.draw(p);
    if(didEquip) { // Your code should start here
        p.getInventory().addItem(LEVITATION_ITEM);
    }
    return didEquip;
}

@Override
public boolean discard(Player p) {
    boolean didDispose = super.discard(p);
    if(didDispose) { // Your code should start here
        Levitation lev = levitators.get(p);
        if(lev != null)
            lev.drop();
        p.getInventory().removeItemAnySlot(LEVITATION_ITEM);
    }
    return didDispose;
}
```
5. You can access some extra Ultimates related data on a `Player` by using: 
```java
CardHolder holder = CardHolder.getCardHolder(Player);
```
6. If you have extra classes pertaining to a card, place them in the `cardhelpers` package.
7. You can get a reference to the main Ultimates class with the variable `plugin`.
8. Remember to check atlas for helpful utils to speed up your development.
