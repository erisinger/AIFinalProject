import java.util.*;
class GeneticSubsumption {
	//weights
	private final int WANDER = 0;
	private final int REST_AT_1 = 1;
	private final int REST_AT_5 = 2;
	private final int REST_AT_10 = 3;
	private final int REST = 4;
	private final int FIND_FOOD = 5;
	private final int SENSE_OF_SMELL = 6;
	
	
	private ArrayList<GSAgents> agents;
	private GSEnvironmentNode[][] grid;
	
	public GeneticSubsumption(GSEnvironmentNode[][] g, int numAgents){
		agents = new ArrayList<GSAgent>();
		grid = g;
		for (i = 0; i < numAgents; i++) {
			agents.add(new GSAgent());
		}
		
		//assign agents to nodes
	}
	
	public void run(int c){
		
		int CYCLES = c;
		int PROGENITORS = 10;
		
		//run for CYCLES generations
		for (int i = 0; i < CYCLES; i++) {
			
			//run until all agents are dead, then cross the most successful
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
					c1.genes.add(genes.get(i);
					c2.genese.add(a.genes.get(i);
				}
				else {
					c2.genes.add(genes.get(i);
					c1.genese.add(a.genes.get(i);
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
			weight = Math.random() * 10;
		}
	}
	
	public class GSEnvironmentNode{
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