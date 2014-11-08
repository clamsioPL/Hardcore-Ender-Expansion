package chylex.hee.world.feature;
import java.util.List;
import java.util.Random;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;
import org.apache.commons.lang3.tuple.Pair;
import chylex.hee.block.BlockList;
import chylex.hee.system.commands.HeeDebugCommand.HeeTest;
import chylex.hee.system.weight.ObjectWeightPair;
import chylex.hee.system.weight.WeightedList;
import chylex.hee.world.feature.blobs.BlobGenerator;
import chylex.hee.world.feature.blobs.BlobPattern;
import chylex.hee.world.feature.blobs.BlobPopulator;
import chylex.hee.world.feature.blobs.generators.BlobGeneratorSingle;
import chylex.hee.world.feature.blobs.populators.BlobPopulatorPlant;
import chylex.hee.world.feature.util.DecoratorFeatureGenerator;
import chylex.hee.world.util.IRandomAmount;

public class WorldGenBlob extends WorldGenerator{
	private enum BlobType{
		COMMON, UNCOMMON, RARE;
		
		WeightedList<BlobPattern> patterns = new WeightedList<>();
	}
	
	private static final WeightedList<ObjectWeightPair<BlobType>> types = new WeightedList<>();
	
	static{
		types.add(ObjectWeightPair.of(BlobType.COMMON,20));
		types.add(ObjectWeightPair.of(BlobType.UNCOMMON,4));
		types.add(ObjectWeightPair.of(BlobType.RARE,1));
		
		BlobType.COMMON.patterns.addAll(new BlobPattern[]{
			
		});
	}
	
	@Override
	public boolean generate(World world, Random rand, int x, int y, int z){
		if (world.getBlock(x-8,y,z) != Blocks.air ||
			world.getBlock(x+8,y,z) != Blocks.air ||
			world.getBlock(x,y,z-8) != Blocks.air ||
			world.getBlock(x,y,z+8) != Blocks.air ||
			world.getBlock(x,y-8,z) != Blocks.air ||
			world.getBlock(x,y+8,z) != Blocks.air)return false;
		
		DecoratorFeatureGenerator gen = new DecoratorFeatureGenerator();

		Pair<BlobGenerator,List<BlobPopulator>> pattern = types.getRandomItem(rand).getObject().patterns.getRandomItem(rand).generatePattern(rand);
		
		pattern.getLeft().generate(gen,rand);
		for(BlobPopulator populator:pattern.getRight())populator.generate(gen,rand);
		
		gen.generate(world,rand,x,y,z);
		return true;
	}
	
	public static final HeeTest $debugTest = new HeeTest(){
		@Override
		public void run(){
			WeightedList<BlobPattern> patterns = new WeightedList<>(new BlobPattern[]{
				new BlobPattern(10).addGenerators(new BlobGenerator[]{
					new BlobGeneratorSingle(1).rad(7.4D,7.4D)
				}).addPopulators(new BlobPopulator[]{
					new BlobPopulatorPlant(1).block(BlockList.death_flower).blockAmount(IRandomAmount.exact,50,50).attempts(90,90).knownBlockLocations()
				}).setPopulatorAmountProvider(IRandomAmount.exact,1,1)
			});
			
			DecoratorFeatureGenerator gen = new DecoratorFeatureGenerator();
			Pair<BlobGenerator,List<BlobPopulator>> pattern = patterns.getRandomItem(world.rand).generatePattern(world.rand);
			
			pattern.getLeft().generate(gen,world.rand);
			for(BlobPopulator populator:pattern.getRight())populator.generate(gen,world.rand);
			
			gen.generate(world,world.rand,(int)player.posX+10,(int)player.posY-5,(int)player.posZ);
		}
	};
}
