package chylex.hee.world.structure.island.biome.feature.island.laboratory;
import java.util.Random;
import chylex.hee.world.structure.util.pregen.LargeStructureWorld;

public final class LaboratoryTerrainAnalyser{
	private final LargeStructureWorld world;
	
	public LaboratoryTerrainAnalyser(LargeStructureWorld world){
		this.world = world;
	}
	
	public LaboratoryTerrainMap generateBestMap(Random rand, int scoreThreshold){
		LaboratoryTerrainMap bestMap = new LaboratoryTerrainMap(world,0,0);
		if (bestMap.getScore() >= scoreThreshold)return bestMap;
		
		for(int x = 0; x < 16; x += 2){
			for(int z = 0; z < 16; z += 2){
				LaboratoryTerrainMap map = new LaboratoryTerrainMap(world,x,z);
				if (map.getScore() > bestMap.getScore() && (bestMap = map).getScore() >= scoreThreshold)return bestMap;
			}
		}
		
		return bestMap;
	}
}