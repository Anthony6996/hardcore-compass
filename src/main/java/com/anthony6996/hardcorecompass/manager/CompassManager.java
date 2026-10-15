package com.anthony6996.hardcorecompass.manager;

import com.anthony6996.hardcorecompass.HardcoreCompassPlugin;
import com.anthony6996.hardcorecompass.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import java.util.List;
import java.util.UUID;

public final class CompassManager {
    private final HardcoreCompassPlugin plugin;
    private final NamespacedKey compassKey, lodestoneKey, lodestoneBlockKey;
    private final NamespacedKey compassRecipeKey, lodestoneRecipeKey;
    private final ConfigManager config;

    public CompassManager(HardcoreCompassPlugin plugin, ConfigManager config) {
        this.plugin=plugin; this.config=config;
        compassKey=new NamespacedKey(plugin,"hardcore_compass");
        lodestoneKey=new NamespacedKey(plugin,"hardcore_lodestone");
        lodestoneBlockKey=new NamespacedKey(plugin,"hardcore_lodestone_block");
        compassRecipeKey=new NamespacedKey(plugin,"hardcore_compass_recipe");
        lodestoneRecipeKey=new NamespacedKey(plugin,"hardcore_lodestone_recipe");
    }
    public NamespacedKey lodestoneBlockKey(){return lodestoneBlockKey;}
    public void registerRecipe(){
        Bukkit.removeRecipe(compassRecipeKey); Bukkit.removeRecipe(lodestoneRecipeKey);
        if(config.recipeEnabled())registerConfigured("compass",compassRecipeKey,createCompass(null,null),false);
        if(config.lodestoneRecipeEnabled())registerConfigured("lodestone",lodestoneRecipeKey,createLodestone(null),true);
    }
    private void registerConfigured(String section,NamespacedKey key,ItemStack result,boolean lodestone){
        try {
            List<String> shape=config.recipeShape(section);
            java.util.Set<Character> symbols=new java.util.HashSet<>();
            if(shape.isEmpty()||shape.size()>3)throw new IllegalArgumentException("shape must contain 1-3 rows");
            for(String row:shape){if(row==null||row.isEmpty()||row.length()>3)throw new IllegalArgumentException("each shape row must contain 1-3 characters");row.chars().filter(ch->ch!=' ').forEach(ch->symbols.add((char)ch));}
            if(symbols.isEmpty())throw new IllegalArgumentException("shape must use at least one ingredient symbol");
            ShapedRecipe recipe=new ShapedRecipe(key,result);recipe.shape(shape.toArray(String[]::new));
            for(char symbol:symbols){Material material=config.recipeIngredient(section,symbol);if(material==null)throw new IllegalArgumentException("invalid/missing material for symbol "+symbol);recipe.setIngredient(symbol,material);}
            if(!Bukkit.addRecipe(recipe))throw new IllegalStateException("Bukkit rejected recipe registration");
        } catch(RuntimeException ex){plugin.getLogger().severe("Disabled invalid recipe.recipe."+section+": "+ex.getMessage()+". Plugin will continue enabling.");}
    }
    public boolean isCompassRecipe(Recipe recipe){return recipe instanceof ShapedRecipe s&&compassRecipeKey.equals(s.getKey());}
    public boolean isLodestoneRecipe(Recipe recipe){return recipe instanceof ShapedRecipe s&&lodestoneRecipeKey.equals(s.getKey());}
    public ItemStack createCompass(Block lodestone){return createCompass(lodestone,null);}
    public ItemStack createCompass(Block lodestone,Player recipient){ItemStack item=new ItemStack(Material.COMPASS);CompassMeta meta=(CompassMeta)item.getItemMeta();Location target=lodestone==null?null:lodestone.getLocation();if(target!=null){meta.setLodestone(target);meta.setLodestoneTracked(true);}String lang=config.language(recipient);meta.setDisplayName(config.localized(lang,"compass-name"));meta.getPersistentDataContainer().set(compassKey,PersistentDataType.BYTE,(byte)1);updateLore(meta,lang,target,false);item.setItemMeta(meta);return item;}
    public ItemStack createLodestone(Player recipient){ItemStack item=new ItemStack(Material.LODESTONE);ItemMeta meta=item.getItemMeta();String lang=config.language(recipient);meta.setDisplayName(config.localized(lang,"lodestone-name"));meta.setLore(List.of(config.localized(lang,"lodestone-lore")));meta.getPersistentDataContainer().set(lodestoneKey,PersistentDataType.BYTE,(byte)1);item.setItemMeta(meta);return item;}
    public boolean isHardcoreCompass(ItemStack item){return item!=null&&item.getType()==Material.COMPASS&&item.hasItemMeta()&&item.getItemMeta().getPersistentDataContainer().has(compassKey,PersistentDataType.BYTE);}
    public boolean isTagged(ItemStack item){return isHardcoreCompass(item);}
    public boolean isHardcoreLodestone(ItemStack item){return item!=null&&item.getType()==Material.LODESTONE&&item.hasItemMeta()&&item.getItemMeta().getPersistentDataContainer().has(lodestoneKey,PersistentDataType.BYTE);}
    public boolean isHardcoreLodestoneBlock(Block block){if(block==null||block.getType()!=Material.LODESTONE||!(block.getState() instanceof TileState tile))return false;return tile.getPersistentDataContainer().has(lodestoneBlockKey,PersistentDataType.BYTE);}
    public void markPlacedLodestone(Block block,UUID owner){if(!(block.getState() instanceof TileState tile))return;PersistentDataContainer pdc=tile.getPersistentDataContainer();pdc.set(lodestoneBlockKey,PersistentDataType.BYTE,(byte)1);if(owner!=null)pdc.set(new NamespacedKey(plugin,"lodestone_owner"),PersistentDataType.STRING,owner.toString());tile.update(true,false);}
    public boolean mayBreak(Block block,Player player){String policy=config.lodestoneBreakProtection();if(policy.equals("none"))return true;if(policy.equals("admin-only"))return player.hasPermission("hardcorecompass.admin");if(policy.equals("owner-only")){if(player.hasPermission("hardcorecompass.admin"))return true;if(!(block.getState() instanceof TileState tile))return false;String value=tile.getPersistentDataContainer().get(new NamespacedKey(plugin,"lodestone_owner"),PersistentDataType.STRING);return value!=null&&value.equals(player.getUniqueId().toString());}return true;}
    public void bind(ItemStack item,Player player,Location target){if(!isHardcoreCompass(item)||!(item.getItemMeta() instanceof CompassMeta meta))return;String lang=config.language(player);meta.setLodestone(target);meta.setLodestoneTracked(true);updateLore(meta,lang,target,false);item.setItemMeta(meta);}
    public void markTargetLost(World world,Location target){for(Player player:Bukkit.getOnlinePlayers()){markContents(player.getInventory().getContents(),player,world,target);markContents(player.getEnderChest().getContents(),player,world,target);}}
    private void markContents(ItemStack[] items,Player player,World world,Location target){for(ItemStack item:items){if(!isHardcoreCompass(item)||!(item.getItemMeta() instanceof CompassMeta meta)||!meta.hasLodestone())continue;Location bound=meta.getLodestone();if(bound==null||bound.getWorld()==null||!bound.getWorld().equals(world)||bound.getBlockX()!=target.getBlockX()||bound.getBlockY()!=target.getBlockY()||bound.getBlockZ()!=target.getBlockZ())continue;meta.setLodestoneTracked(false);updateLore(meta,config.language(player),null,true);item.setItemMeta(meta);}}
    public void refreshCompass(ItemStack item,Player player){if(!isHardcoreCompass(item)||!(item.getItemMeta() instanceof CompassMeta meta))return;Location target=meta.hasLodestone()?meta.getLodestone():null;boolean lost=target!=null&&!targetExists(target);if(lost)meta.setLodestoneTracked(false);updateLore(meta,config.language(player),lost?null:target,lost);item.setItemMeta(meta);}
    private boolean targetExists(Location target){World w=target.getWorld();if(w==null)return false;Block block=w.getBlockAt(target);return block.getType()==Material.LODESTONE;}
    public void refreshPlayer(Player player){for(ItemStack item:player.getInventory().getContents())refreshCompass(item,player);for(ItemStack item:player.getEnderChest().getContents())refreshCompass(item,player);}
    public void wipeLinks(Player player){for(ItemStack item:player.getInventory().getContents())clearLink(item,player);for(ItemStack item:player.getEnderChest().getContents())clearLink(item,player);}
    private void clearLink(ItemStack item,Player player){if(!isHardcoreCompass(item)||!(item.getItemMeta() instanceof CompassMeta meta))return;meta.setLodestone(null);meta.setLodestoneTracked(false);updateLore(meta,config.language(player),null,false);item.setItemMeta(meta);}
    public ItemStack findTaggedCompass(Player player){for(ItemStack item:player.getInventory().getContents())if(isHardcoreCompass(item))return item.clone();return null;}
    public void giveStarter(Player player){if(!config.starterEnabled())return;if(config.starterLodestone())player.getInventory().addItem(createLodestone(player));if(config.starterCompass())player.getInventory().addItem(createCompass(null,player));}
    private void updateLore(CompassMeta meta,String lang,Location target,boolean lost){String key=lost?"compass-lost":target==null?"compass-unbound":"compass-bound-lore";String text=config.localized(lang,key);if(target!=null)text=text.replace("{x}",String.valueOf(target.getBlockX())).replace("{y}",String.valueOf(target.getBlockY())).replace("{z}",String.valueOf(target.getBlockZ()));else text=text.replace("{lodestone_name}",config.localized(lang,"lodestone-name"));meta.setLore(List.of(text));}
}
