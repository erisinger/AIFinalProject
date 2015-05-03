import java.util.*;
class GeneticSubsumption {
	
	private ArrayList<GSAgent> agents;
	private GSEnvironmentNode[][] grid;
	
	private int gridHeight = 100;
	private int gridWidth = 100;
	
	public GeneticSubsumption(int numAgents){
		
		agents = new ArrayList<GSAgent>();
		generateGrid(gridHeight, gridWidth);
		
		int count = numAgents;
		while (count > 0) {
			agents.add(new GSAgent(8));
			count--;
		}				
		
		assignAgentsToNodes();
	}
	
	private void generateGrid(int h, int w){
		
		GSEnvironmentNode[][] g = new GSEnvironmentNode[h][w];
		for (int i = 0; i < h; i++) {
			for (int j = 0; j < w; j++) {
				GSEnvironmentNode n = new GSEnvironmentNode(i, j, h, w);
				if (Math.random() < 0.1) {
					n.hasFood = true;
				}
				g[i][j] = n;
			}
		}	
		this.grid = g;	
	}
	
	private void assignAgentsToNodes(){
		int tempI, tempJ;
		for (GSAgent a : agents) {
			tempI = (int)(Math.random() * (double)grid.length);
			tempJ = (int)(Math.random() * (double)grid[0].length);
			grid[tempI][tempJ].agents.add(a);
		}	
	}
	
	public void run(int epochs){
		System.out.println("running...");
		
		int CYCLES = epochs;
		int PROGENITORS = 0;
		
		//run for CYCLES generations
		for (int k = 0; k < CYCLES; k++) {
			System.out.println("\ncycle " + k);
			
			//reset game
			generateGrid(gridHeight, gridWidth);
			for (GSAgent a : agents) {
				a.resetStats();
			}
			assignAgentsToNodes();
//			System.out.println(toString());
			
			//run until all agents are dead, then cross the most successful (longevity-wise)
			while (agentsRemaining() > PROGENITORS) {
//				System.out.println("\nagents left: " + agentsRemaining());
				
				//for each node in the grid, check the node for agents
				for (int i = 0; i < grid.length; i++) {
					for (int j = 0; j < grid[0].length; j++) {
						GSEnvironmentNode n = grid[i][j];
						
						ArrayList<GSAgent> tempAgents = new ArrayList<GSAgent>();
						for (GSAgent a : n.agents) {
							tempAgents.add(a);
						}
							
						for (GSAgent a : tempAgents) {
							//tell agent its location
							n.agents.remove(a);
							a.act(n, grid);
							
							//assign to new location
							if (a.health > 0) {
								grid[a.node.i][a.node.j].agents.add(a);
							}
							else {
//								System.out.println("agent died with lifespan " + a.lifeSpan);
							}
							a.node = null;
							
						}
					}
				}
			}
			
			//cross agents
			Collections.sort(agents);
			GSAgent victor = agents.get(0);
			System.out.println("victor: lifespan: " + victor.lifeSpan);
			
			ArrayList<GSAgent> progenitors = new ArrayList<GSAgent>();
			for (int i = 0; i < 10 && i < agents.size(); i++) {
				progenitors.add(agents.get(i));
			}
			
			for (int i = 1; i < progenitors.size(); i++) {
				for (GSAgent c : progenitors.get(i).crossWith(progenitors.get(i - 1))) {
					agents.add(c);
				}
			}
			//start again...			
		}
		System.out.println("run complete");
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
	
	public String toString(){
		String str = "";
		for (int i = 0; i < grid.length; i++) {
			for (int j = 0; j < grid[0].length; j++) {
				str += " " + grid[i][j].agents.size() + ":" + (grid[i][j].hasFood ? "x" : "o");
			}
			str += "\n";
		}
		str += "\n";
		
		return str;
	}
	
	
	/* AGENT CLASS */
	
	public class GSAgent implements Comparable<GSAgent>{
		
		//gene weights
		private int	WANDER = 0;				//probability of undirected movement
		private int	DIRECTED_SEARCH = 1;	//probability of directed movement
		private int	LAZINESS = 2;			//probability of no movement
		private int	ENDURANCE = 3;			//ability to go without rest
		private int	SENSE_OF_SMELL = 4;		//probability of detecting food in neighboring squares
		private int	APPETITE = 5;			//probability of moving toward food once detected
		private int	METABOLISM = 6;			//frequency of eating required to maintain health
		private int	RECOVERY_RATE = 7;		//amount of rest required to recover
		
		//value to maximize
		public double health = 1;
		
		//other health-related stats
		public long lifeSpan = 0;
		
		//rest
		private int cyclesSinceRest = 0;
		private int consecutiveRestCycles = 0;
		private double fatigue = 0;
		
		//food
		private int cyclesSinceEating = 0;
		private double hunger = 0;
		
		//directional stats
		private int xHeading = 0;
		private int yHeading = 0;
		
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
		
		//constructor for empty agent
		public GSAgent(){
			 
		}
		
		public void act(GSEnvironmentNode n, GSEnvironmentNode[][] grid){
//			System.out.println("act() with health: " + health);
			
			//if agent is dead, do nothing
			if (health <= 0) {
				health = 0;
				return;
			}
			
			//agent is not dead
			int nextX = 0;
			int nextY = 0;
			
			//get current location
			node = n;
			
			/* update stats */
			
			//update fatigue
			if (xHeading == 0 && yHeading == 0) {
				//agent didn't move last cycle
				cyclesSinceRest = 0;
				fatigue -= genes.get(RECOVERY_RATE).weight * consecutiveRestCycles;
			}
			else {
				cyclesSinceRest++;
				fatigue += (1 - genes.get(ENDURANCE).weight) * cyclesSinceRest;
			}
			
			//update hunger: hunger += cycles since eating scaled by metabolism
			cyclesSinceEating++;
			hunger += cyclesSinceEating * genes.get(METABOLISM).weight;
			
			//update health
			double healthMultiplier = 0.01;
			health -= healthMultiplier * (hunger + fatigue);
			
			//if agent is still alive, move and increment lifespan
			if (health > 0) {
				move();
				lifeSpan++;				
			}
			else {
				health = 0;
			}

		}
		
		private void move(){
			int nextX = node.j;
			int nextY = node.i;
			
//			if (Math.random() > 0.5) {
//				xHeading = randomDirection();
//				yHeading = randomDirection();	
//			}

			//food detected nearby?
			GSEnvironmentNode neighboringNodeWithFood = null;
			int smellRadius = 5;
			boolean foodInSquare = false;

			if (node.hasFood) {
				if (genes.get(APPETITE).weight > Math.random()) {
					node.hasFood = false;
					cyclesSinceEating = 0;
					hunger -= (1 - genes.get(METABOLISM).weight);
				}
			}
			else {
				for (int i = -smellRadius; i <= smellRadius; i++) {
					for (int j = -smellRadius; j <= smellRadius; j++) {
						if (node.i + i >= 0 && node.i + i < node.height && node.j + j >= 0 && node.j + j < node.width) {
							if (grid[node.i + i][node.j + j].hasFood) {
								neighboringNodeWithFood = grid[node.i + i][node.j + j];
							}
						}
					}
				}				
			}

			//calculate direction of food
			if (neighboringNodeWithFood != null) {
				if (genes.get(APPETITE).weight > Math.random()) {
					
					//food vector
					int foodI = neighboringNodeWithFood.i - node.i;
					int foodJ = neighboringNodeWithFood.j - node.j;
					
					//normalize
					if (foodI != 0) {
						foodI = foodI / Math.abs(foodI);
					}
					if (foodJ != 0) {
						foodJ = foodJ / Math.abs(foodJ);
					}
					
					yHeading = foodI;
					xHeading = foodJ;
					
//					System.out.println("changed heading to " + yHeading + ", " + xHeading);
				}
			}
			
			//next i
			if (node.i + yHeading >= 0 && node.i + yHeading < grid.length) {
				nextY = node.i + yHeading;
			}
			
			//next j
			if (node.j + xHeading >= 0 && node.j + xHeading < grid[0].length) {
				nextX = node.j + xHeading;
			}
			
			/* does wandering, searching, moving toward food or laziness win? */
			
			node = grid[nextY][nextX];
//			System.out.println("moved to " + nextY + ", " + nextX);

		}
		
		private int randomDirection(){
			int direction = Math.random() > 0.5 ? 1 : 0;
			return Math.random() > 0.5 ? direction : -direction;
		}
		
		public ArrayList<GSAgent> crossWith(GSAgent a){
			//children
			GSAgent c1 = new GSAgent();
			GSAgent c2 = new GSAgent();
			
			//crossover
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
			
			//mutation
			double mutationProbability = 0.2;
			for (GSAGene g : c1.genes) {
				if (Math.random() < mutationProbability) {
					g.weight = Math.random();
				}
			}
			
			for (GSAGene g : c2.genes) {
				if (Math.random() < mutationProbability) {
					g.weight = Math.random();
				}
			}

			
			//add offspring to population
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
		
		public void resetStats(){
			health = 1;
			lifeSpan = 0;
			cyclesSinceRest = 0;
			consecutiveRestCycles = 0;
			fatigue = 0;
			cyclesSinceEating = 0;
			hunger = 0;
			xHeading = 0;
			yHeading = 0;		
		}
		
		public String toString(){
			String str = "health: " + health + "\n";
			for (GSAGene g : genes) {
				str += g.name + ": " + g.weight + "\n";
			}	
			return str;
		}	
		
		public int compareTo(GSAgent o){
			if (this.lifeSpan > o.lifeSpan) {
				return -1;
			}
			else if (this.lifeSpan < o.lifeSpan) {
				return 1;
			}
			else {
				return 0;
			}
		}
		
	}
	
	public static class GSAGene{
		public String name = "";
		public double weight = 0.5;
		
		//assigned values
		public GSAGene(String n, int w){
			name = n;
			weight = w;
		}
		
		//default values
		public GSAGene(){
			
		}
		
		public void randomize(){
			weight = Math.random();
		}
	}
	
	public static class GSEnvironmentNode{
		public boolean hasFood = false;
		public ArrayList<GSAgent> agents = new ArrayList<GSAgent>();
		public int i;
		public int j;
		public int height, width;
		
		public GSEnvironmentNode(int m, int n, int h, int w){
			i = m;
			j = n;
			height = h;
			width = w;
		}
		
	}
	
	public static void main(String[] args) {
		int AGENTS = 100;
		int EPOCHS = 10;
		
		//initialize with AGENTS agents
		GeneticSubsumption gs = new GeneticSubsumption(AGENTS);
		
		//run for EPOCHS cycles
		gs.run(EPOCHS);
		
	}
}