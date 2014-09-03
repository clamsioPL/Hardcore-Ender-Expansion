package chylex.hee.mechanics.compendium.player;
import org.apache.commons.lang3.StringUtils;
import gnu.trove.set.hash.TIntHashSet;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;
import net.minecraftforge.common.util.Constants.NBT;
import chylex.hee.mechanics.compendium.content.objects.IKnowledgeObjectInstance;
import chylex.hee.mechanics.compendium.content.objects.ObjectBlock;
import chylex.hee.mechanics.compendium.content.objects.ObjectBlock.BlockMetaWrapper;
import chylex.hee.mechanics.compendium.content.objects.ObjectItem;
import chylex.hee.mechanics.compendium.content.objects.ObjectMob;
import chylex.hee.mechanics.compendium.content.type.KnowledgeFragment;
import chylex.hee.mechanics.compendium.content.type.KnowledgeObject;
import chylex.hee.mechanics.compendium.player.PlayerDiscoveryList.IObjectSerializer;
import chylex.hee.system.logging.Stopwatch;
import cpw.mods.fml.common.registry.GameData;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.GameRegistry.UniqueIdentifier;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class PlayerCompendiumData implements IExtendedEntityProperties{
	private int pointAmount;
	
	private PlayerDiscoveryList<ObjectBlock,BlockMetaWrapper> discoveredBlocks = new PlayerDiscoveryList<>(new DiscoveryBlockSerializer());
	private PlayerDiscoveryList<ObjectItem,Item> discoveredItems = new PlayerDiscoveryList<>(new DiscoveryItemSerializer());
	private PlayerDiscoveryList<ObjectMob,Class<? extends EntityLivingBase>> discoveryMobs = new PlayerDiscoveryList<>(new DiscoveryMobSerializer());
	
	private TIntHashSet unlockedFragments = new TIntHashSet();
	
	public PlayerCompendiumData(){}
	
	@SideOnly(Side.CLIENT)
	public PlayerCompendiumData(NBTTagCompound nbt){
		loadNBTData(nbt);
	}
	
	public int getPoints(){
		return pointAmount;
	}
	
	public void givePoints(int amount){
		pointAmount += amount;
	}
	
	public void payPoints(int amount){
		pointAmount = Math.max(0,pointAmount-amount);
	}
	
	public boolean tryDiscoverObject(KnowledgeObject<?> object, boolean addReward){
		IKnowledgeObjectInstance<?> obj = object.getObject();
		
		if (obj instanceof ObjectBlock)return tryDiscoverBlock((KnowledgeObject<ObjectBlock>)object,addReward);
		else if (obj instanceof ObjectItem)return tryDiscoverItem((KnowledgeObject<ObjectItem>)object,addReward);
		else if (obj instanceof ObjectMob)return tryDiscoverMob((KnowledgeObject<ObjectMob>)object,addReward);
		else return false;
	}
	
	public boolean hasDiscoveredObject(KnowledgeObject<?> object){
		IKnowledgeObjectInstance<?> obj = object.getObject();
		
		if (obj instanceof ObjectBlock)return discoveredBlocks.hasDiscoveredObject((ObjectBlock)obj);
		else if (obj instanceof ObjectItem)return discoveredItems.hasDiscoveredObject((ObjectItem)obj);
		else if (obj instanceof ObjectMob)return discoveryMobs.hasDiscoveredObject((ObjectMob)obj);
		else return false;
	}
	
	public boolean tryDiscoverBlock(KnowledgeObject<ObjectBlock> block, boolean addReward){ System.out.println("discover block "+block.getObject().getUnderlyingObject());
		if (discoveredBlocks.addObject(block.getObject())){
			onDiscover(block,addReward);
			return true;
		}
		else return false;
	}
	
	public boolean hasDiscoveredBlock(KnowledgeObject<ObjectBlock> block){
		return discoveredBlocks.hasDiscoveredObject(block.getObject());
	}
	
	public boolean tryDiscoverItem(KnowledgeObject<ObjectItem> item, boolean addReward){ System.out.println("discover item "+item.getObject().getUnderlyingObject());
		if (discoveredItems.addObject(item.getObject())){
			onDiscover(item,addReward);
			return true;
		}
		else return false;
	}
	
	public boolean hasDiscoveredItem(KnowledgeObject<ObjectItem> item){
		return discoveredItems.hasDiscoveredObject(item.getObject());
	}
	
	public boolean tryDiscoverMob(KnowledgeObject<ObjectMob> mob, boolean addReward){ System.out.println("discover mob "+mob.getObject().getUnderlyingObject().getSimpleName());
		if (discoveryMobs.addObject(mob.getObject())){
			onDiscover(mob,addReward);
			return true;
		}
		else return false;
	}
	
	public boolean hasDiscoveredMob(KnowledgeObject<ObjectMob> mob){
		return discoveryMobs.hasDiscoveredObject(mob.getObject());
	}
	
	private void onDiscover(KnowledgeObject<?> object, boolean addReward){
		if (addReward){
			unlockDiscoveryFragments(object);
			pointAmount += object.getDiscoveryReward();
		}
	}
	
	private void unlockDiscoveryFragments(KnowledgeObject<?> object){
		for(KnowledgeFragment fragment:object.getFragments()){
			if (fragment.isUnlockedOnDiscovery())unlockFragment(fragment);
		}
	}
	
	public boolean unlockFragment(KnowledgeFragment fragment){
		if (unlockedFragments.add(fragment.globalID)){
			for(int cascade:fragment.getUnlockCascade())unlockFragment(KnowledgeFragment.getById(cascade));
			return true;
		}
		else return false;
	}
	
	public boolean hasUnlockedFragment(KnowledgeFragment fragment){
		return unlockedFragments.contains(fragment.globalID);
	}
	
	public FragmentPurchaseStatus canPurchaseFragment(KnowledgeFragment fragment){
		if (!fragment.isBuyable())return FragmentPurchaseStatus.NOT_BUYABLE;
		if (pointAmount < fragment.getPrice())return FragmentPurchaseStatus.NOT_ENOUGH_POINTS;
		
		for(int requirement:fragment.getUnlockRequirements()){
			if (!unlockedFragments.contains(requirement))return FragmentPurchaseStatus.REQUIREMENTS_UNFULFILLED;
		}
		
		return FragmentPurchaseStatus.CAN_PURCHASE;
	}
	
	@Override
	public void init(Entity entity, World world){}
	
	@Override
	public void saveNBTData(NBTTagCompound nbt){
		Stopwatch.time("PlayerCompendiumData - save");
		NBTTagCompound hee = new NBTTagCompound();
		hee.setInteger("kps",pointAmount);
		hee.setTag("fndBlocks",discoveredBlocks.saveToNBTList());
		hee.setTag("fndItems",discoveredItems.saveToNBTList());
		hee.setTag("fndMobs",discoveryMobs.saveToNBTList());
		hee.setTag("unlocked",new NBTTagIntArray(unlockedFragments.toArray()));
		nbt.setTag("HEE",hee);
		Stopwatch.finish("PlayerCompendiumData - save");
	}

	@Override
	public void loadNBTData(NBTTagCompound nbt){
		Stopwatch.time("PlayerCompendiumData - load");
		NBTTagCompound hee = nbt.getCompoundTag("HEE");
		pointAmount = hee.getInteger("kps");
		discoveredBlocks.loadFromNBTList(hee.getTagList("fndBlocks",NBT.TAG_STRING));
		discoveredItems.loadFromNBTList(hee.getTagList("fndItems",NBT.TAG_STRING));
		discoveryMobs.loadFromNBTList(hee.getTagList("fndMobs",NBT.TAG_STRING));
		unlockedFragments.addAll(hee.getIntArray("unlocked"));
		Stopwatch.finish("PlayerCompendiumData - load");
	}
	
	// SERIALIZERS
	
	private static class DiscoveryBlockSerializer implements IObjectSerializer<BlockMetaWrapper>{
		@Override
		public String serialize(BlockMetaWrapper object){
			UniqueIdentifier identifier = GameRegistry.findUniqueIdentifierFor(object.block);
			return identifier.modId+":"+identifier.name+":"+object.metadata;
		}

		@Override
		public BlockMetaWrapper deserialize(String data){
			int secondColonIndex = data.indexOf(":",data.indexOf(":")+1);
			String meta = data.substring(secondColonIndex+1);
			
			if (!meta.equals("-1") && !StringUtils.isNumeric(meta))return null;
			else return new BlockMetaWrapper(GameData.getBlockRegistry().getObject(data.substring(0,secondColonIndex)),Integer.parseInt(meta));
		}
	}
	
	private static class DiscoveryItemSerializer implements IObjectSerializer<Item>{
		@Override
		public String serialize(Item object){
			UniqueIdentifier identifier = GameRegistry.findUniqueIdentifierFor(object);
			return identifier.modId+":"+identifier.name;
		}

		@Override
		public Item deserialize(String data){
			return GameData.getItemRegistry().getObject(data);
		}
	}
	
	private static class DiscoveryMobSerializer implements IObjectSerializer<Class<? extends EntityLivingBase>>{
		@Override
		public String serialize(Class<? extends EntityLivingBase> object){
			return (String)EntityList.classToStringMapping.get(object);
		}

		@Override
		public Class<? extends EntityLivingBase> deserialize(String data){
			return (Class<? extends EntityLivingBase>)EntityList.stringToClassMapping.get(data);
		}
	}
	
	// ENUMS
	
	public enum FragmentPurchaseStatus{
		CAN_PURCHASE, NOT_BUYABLE, NOT_ENOUGH_POINTS, REQUIREMENTS_UNFULFILLED
	}
}