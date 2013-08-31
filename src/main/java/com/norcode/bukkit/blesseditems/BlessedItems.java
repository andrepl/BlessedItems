package com.norcode.bukkit.blesseditems;

import net.minecraft.server.v1_6_R2.ContainerAnvil;
import net.minecraft.server.v1_6_R2.ContainerAnvilInventory;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_6_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_6_R2.inventory.CraftInventoryAnvil;
import org.bukkit.craftbukkit.v1_6_R2.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.*;

public class BlessedItems extends JavaPlugin implements Listener  {

    private DataStore datastore;

    @Override
    public void onEnable() {
        datastore = new DataStore(this, "data.yml");
        getServer().getPluginManager().registerEvents(this, this);
    }

    public void onDisable() {
        saveConfig(); // Save any items left in limbo on server shutdown.
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command may only be run by a player.");
            return true;
        }
        Player player = (Player) sender;
        player.getInventory().addItem(createBlessingDeed());

//
//        Player player = (Player) sender;
//        if (player.getItemInHand() == null) {
//            sender.sendMessage("You must be holding an item to use this command.");
//            return true;
//        }
//        ItemMeta meta = player.getItemInHand().getItemMeta();
//        if (meta == null) {
//            meta = getServer().getItemFactory().getItemMeta(player.getItemInHand().getType());
//        }
//        List<String> lore = meta.getLore();
//        if (lore == null) {
//            lore = new ArrayList<String>();
//        }
//        lore.add(ChatColor.GOLD + "Blessed by " + sender.getName());
//        meta.setLore(lore);
//        player.getItemInHand().setItemMeta(meta);
//        sender.sendMessage("This item has been blessed.");
        return true;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player p = event.getEntity();
        PlayerInventory inv = p.getInventory();

        ItemStack s;
        HashMap<Integer, ItemStack> blessed = new HashMap<Integer, ItemStack>();
        for (int i=0; i<inv.getSize(); i++) {
            s = inv.getItem(i);
            String playerName = itemBlessedBy(s);
            if (playerName != null && event.getEntity().getName().equals(playerName)) {
                blessed.put(i, s);
                inv.remove(i);
                event.getDrops().remove(s);
            }
        }
        for (int i=0;i<inv.getArmorContents().length;i++) {
            int idx = -(i+1);
            getLogger().info(idx + " -> " + inv.getArmorContents()[i]);
            String blesser = itemBlessedBy(inv.getArmorContents()[i]);
            if (blesser != null && p.getName().equals(blesser)) {
                event.getDrops().remove(inv.getArmorContents()[i]);
                blessed.put(idx, inv.getArmorContents()[i]);
                inv.getArmorContents()[i] = null;
            }
        }
        datastore.saveItems(p, blessed);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player p = event.getPlayer();
        for (Map.Entry<Integer, ItemStack> entry: datastore.getItems(p).entrySet()) {
            int idx = entry.getKey();
            getLogger().info("Putting " + entry.getValue() + " in slot " + entry.getKey());
            if (idx < 0) {
                idx = Math.abs(idx + 1);
                p.getEquipment().getArmorContents()[idx] = entry.getValue();
                switch (idx) {
                    case 0:
                        p.getEquipment().setBoots(entry.getValue());
                        break;
                    case 1:
                        p.getEquipment().setLeggings(entry.getValue());
                        break;
                    case 2:
                        p.getEquipment().setChestplate(entry.getValue());
                        break;
                    case 3:
                        p.getEquipment().setHelmet(entry.getValue());
                        break;
                }
            } else {
                p.getInventory().setItem(entry.getKey(), entry.getValue());
            }
        }
        datastore.clearItems(p);
    }

    private String itemBlessedBy(ItemStack s) {
        if (s != null && s.hasItemMeta() && s.getItemMeta().hasLore()) {
            for (String line: s.getItemMeta().getLore()) {
                if (line.startsWith(ChatColor.GOLD + "Blessed by ")) {
                    return line.substring(13);
                }
            }
        }
        return null;
    }

    private boolean isBlessingDeed(ItemStack stack) {
        if (stack != null && stack.getType().equals(Material.PAPER) && stack.hasItemMeta()) {
            ItemMeta meta = stack.getItemMeta();
            return meta.hasLore() && meta.getLore().contains(ChatColor.GRAY + "A Bless Deed");
        }
        return false;
    }

    public ItemStack createBlessingDeed() {
        ItemStack stack = new ItemStack(Material.PAPER);
        ItemMeta meta = getServer().getItemFactory().getItemMeta(Material.PAPER);
        meta.setDisplayName("Bless Deed");
        List<String> lore = new ArrayList<String>();
        lore.add(ChatColor.GRAY + "A Bless Deed");
        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }


    @EventHandler(ignoreCancelled=true, priority= EventPriority.HIGHEST)
    public void onInventoryClick(final InventoryClickEvent event) {
        getServer().getScheduler().runTaskLater(this, new Runnable() {
            @Override
            public void run() {
                if (event.getInventory() instanceof AnvilInventory) {
                    Player player = (Player) event.getWhoClicked();
                    AnvilInventory ai = (AnvilInventory) event.getInventory();
                    ItemStack first = ai.getItem(0);
                    ItemStack second = ai.getItem(1);
                    net.minecraft.server.v1_6_R2.ItemStack nmsResult = ((CraftInventoryAnvil)ai).getResultInventory().getItem(0);
                    ItemStack result = nmsResult == null ? null : CraftItemStack.asCraftMirror(nmsResult);
                    if (first != null && second != null && result == null) {
                        if (first.getMaxStackSize() == 1 && isBlessingDeed(second)) {
                            ItemStack resultStack = first.clone();
                            ContainerAnvilInventory nmsInv = (ContainerAnvilInventory) ((CraftInventoryAnvil) ai).getInventory();
                            try {
                                Field containerField = ContainerAnvilInventory.class.getDeclaredField("a");
                                containerField.setAccessible(true);
                                ContainerAnvil anvil = (ContainerAnvil) containerField.get(nmsInv);

                                anvil.a = 10; // TODO XP COST
                                ItemMeta meta = resultStack.getItemMeta();
                                if (meta == null) {
                                    meta = getServer().getItemFactory().getItemMeta(first.getType());
                                }
                                List<String> lore = meta.getLore();
                                if (lore == null) {
                                    lore = new ArrayList<String>();
                                }
                                lore.add(ChatColor.GOLD + "Blessed by " + event.getWhoClicked().getName());
                                meta.setLore(lore);
                                resultStack.setItemMeta(meta);
                                ((CraftInventoryAnvil)ai).getResultInventory().setItem(0, CraftItemStack.asNMSCopy(resultStack));
                                ((CraftPlayer) player).getHandle().setContainerData(anvil, 0, anvil.a);

                            } catch (NoSuchFieldException e) {
                                e.printStackTrace();
                            } catch (SecurityException e) {
                                e.printStackTrace();
                            } catch (IllegalArgumentException e) {
                                e.printStackTrace();
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }, 0);
    }
}
