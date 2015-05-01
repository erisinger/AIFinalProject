import java.util.*;
class GeneticSubsumption {
	
	//weight indices
	private final int WANDER = 0;
	private final int DIRECTED_SEARCH = 1;
	private final int REST = 2;
	private final int SENSE_OF_SMELL = 3;
	private final int MOVE_TOWARD_FOOD = 4;
	
	
	private ArrayList<GSAgent> agents;
	private GSEnvironmentNode[][] grid;
	
	public GeneticSubsumption(GSEnvironmentNode[][] g, int numAgents){
		int count = numAgents;
		agents = new ArrayList<GSAgent>();
		grid = g;
		
		for (int i = 0; i < grid.length && count > 0; i++) {
			for (int j = 0; j < grid[0].length && count > 0; j++) {
				agents.add(new GSAgent(8));
			}
			
		}
		
		//TO DO: assign agents to nodes
		
	}
	
	public void run(int cyc){
		
		int CYCLES = cyc;
		int PROGENITORS = 10;
		
		//run for CYCLES generations
		for (int k = 0; k < CYCLES; k++) {
			
			//run until almost all agents are dead, then cross the survivors
			while (agentsRemaining() > PROGENITORS) {
				
				//for each node in the grid, check the node for agents
				for (int i = 0; i < grid.length; i++) {
					for (int j = 0; j < grid[0].length; j++) {
						GSEnvironmentNode n = grid[i][j];
						for (GSAgent a : n.agents) {
							
							//tell agent its location
							n.agents.remove(a);
							a.act(n);
							
							//assign to new location
							grid[a.node.i][a.node.j].agents.add(a);
						}
					}
				}
			}
			
			//cross agents -- REPLACE WITH BETTER SELECTION ALGORITHM
			for (int i = 1; i < agents.size(); i++) {
				for (GSAgent c : agents.get(i).crossWith(agents.get(i - 1))) {
					agents.add(c);
				}
			}
			//start again...
		}
	}
	
	private int agentsRemaining(){
		int count = 0;
		for (GSAgent a : agents) {
			if (a.health > 0) {
				count++;
			}
		}
		return count;
	}
	
	public class GSAgent{
		
		//value to maximize
		public double health = 10;
		
		//chromosome
		public ArrayList<GSAGene> genes = new ArrayList<GSAGene>();
		
		//current location
		GSEnvironmentNode node;
		
		private int consecutiveSearchUnits = 0;
		private int searchX = 0;
		private int searchY = 0;
		
		private int cyclesSinceEating = 0;
		
		//constructor for pre-made chromosome
		public GSAgent(ArrayList<GSAGene> g){
			genes = g;
		}
		
		//constructor for default-valued genes
		public GSAgent(int numGenes){
			for (int i = 0; i < numGenes; i++) {
				genes.add(new GSAGene());
			}
		}
		
		//constructor for empty agent
		public GSAgent(){
			
		}
		
		public void act(GSEnvironmentNode n){
			node = n;
			
			//make a choice based on location, nearby objects, etc., and behavioral weights
			
		}
		
		public ArrayList<GSAgent> crossWith(GSAgent a){
			//children
			GSAgent c1 = new GSAgent();
			GSAgent c2 = new GSAgent();
			
			for (int i = 0; i < genes.size(); i++) {
				if (Math.random() > 0.5) {
					c1.genes.add(genes.get(i));
					c2.genes.add(a.genes.get(i));
				}
				else {
					c2.genes.add(genes.get(i));
					c1.genes.add(a.genes.get(i));
				}
			}
			
			ArrayList<GSAgent> children = new ArrayList<GSAgent>();
			children.add(c1);
			children.add(c2);
			
			return children;
		}
		
		public void randomize(){
			for (GSAGene g : genes) {
				g.randomize();
			}
		}	
		
		public String toString(){
			String str = "health: " + health + "\n";
			for (GSAGene g : genes) {
				str += g.name + ": " + g.weight + "\n";
			}	
			return str;
		}	
		
	}
	
	public class GSAGene{
		public String name = "";
		public int weight = 5;
		
		//assigned values
		public GSAGene(String n, int w){
			name = n;
			weight = w;
		}
		
		//default values
		public GSAGene(){
			
		}
		
		public void randomize(){
			weight = (int)Math.random() * 10;
		}
	}
	
	public static class GSEnvironmentNode{
		public boolean hasFood = false;
		public ArrayList<GSAgent> agents = new ArrayList<GSAgent>();
		public int i;
		public int j;
		
		public GSEnvironmentNode(int m, int n){
			i = m;
			j = n;
		}
		
	}
	
	public static void main(String[] args) {
		int h = 100, w = 100;
		
		GSEnvironmentNode[][] grid = new GSEnvironmentNode[h][w];
		for (int i = 0; i < h; i++) {
			for (int j = 0; j < w; j++) {
				GSEnvironmentNode n = new GSEnvironmentNode(i, j);
				if (Math.random() > 0.9) {
					n.hasFood = true;
				}
				grid[i][j] = n;
			}
		}
		
		GeneticSubsumption gs = new GeneticSubsumption(grid, 10);
		gs.run(1000);
		
	}
}