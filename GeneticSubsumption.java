import java.util.*;
class GeneticSubsumption {
	
	private ArrayList<GSAgent> agents = new ArrayList<GSAgent>();
	private GSEnvironmentNode[][] grid;
	private int gridHeight;
	private int gridWidth;
	
	public GeneticSubsumption(int h, int w, int numAgents){
		gridHeight = h;
		gridWidth = w;
		
		//set up a new game
		generateGrid(gridHeight, gridWidth);
		generateAgents(numAgents);
		assignAgentsToNodes();
	}
	
	private void generateAgents(int numAgents){
		int count = numAgents;
		GSAgent a;
		while (count > 0) {
			a = new GSAgent(8);
			a.randomize();
			agents.add(a);
			count--;
		}				
	}
	
	private void generateGrid(int h, int w){
		GSEnvironmentNode[][] g = new GSEnvironmentNode[h][w];
		for (int i = 0; i < h; i++) {
			for (int j = 0; j < w; j++) {
				GSEnvironmentNode n = new GSEnvironmentNode(i, j);
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

//		System.out.println("running...");
		
		//stop when there are PROGENITORS agents remaining -- 0 kills all agents before reproducing
		int PROGENITORS = 0;
		
		//run for specified number of generations
		for (int k = 0; k < epochs; k++) {
//			System.out.println("\nepoch " + k);
			
			//set/reset game
			generateGrid(gridHeight, gridWidth);
			for (GSAgent a : agents) {
				a.resetStats();
			}
			assignAgentsToNodes();
			
			//run until all agents are dead, then cross the most successful (longevity-wise)
			while (agentsRemaining() > PROGENITORS) {
				
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
								//soylent green
								n.hasFood = true;
							}
							a.node = null;
						}
					}
				}
			}
				
			//reproduce using top n agents
			crossAgents(10);

			//start again...			
		}
//		System.out.println("run complete");
	}
	
	private void crossAgents(int numParents){
		Collections.sort(agents);
		GSAgent victor = agents.get(0);
		System.out.println(victor.toString());
		
		ArrayList<GSAgent> progenitors = new ArrayList<GSAgent>();
		for (int i = 0; i < numParents && i < agents.size(); i++) {
			progenitors.add(agents.get(i));
		}
		
		//cull after sorting and before crossover
		for (int i = 0; i < numParents; i++) {
			agents.remove(agents.size() - 1);
		}
		
		for (int i = 1; i < progenitors.size(); i++) {
			for (GSAgent c : progenitors.get(i).crossWith(progenitors.get(i - 1))) {
				agents.add(c);
			}
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
	
	public String toString(){
		String str = "";
		for (int i = 0; i < grid.length; i++) {
			for (int j = 0; j < grid[0].length; j++) {
				str += grid[i][j].agents.size();
			}
			str += "\n";
		}
		
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
				//agent hasn't moved since last cycle
				cyclesSinceRest = 0;
				
				//cap consecutive rest cycles at 10
				consecutiveRestCycles++;
				consecutiveRestCycles = consecutiveRestCycles > 10 ? 10 : consecutiveRestCycles + 1;
				fatigue -= genes.get(RECOVERY_RATE).weight * consecutiveRestCycles;
			}
			else {
				//agent moved -- no rest
				consecutiveRestCycles = 0;
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
			
			/* does wandering, searching, moving toward food or laziness win? */
			double appetiteWeight = genes.get(APPETITE).weight;
			double wanderWeight = genes.get(WANDER).weight;
			double searchWeight = genes.get(DIRECTED_SEARCH).weight;
			if (appetiteWeight > wanderWeight && appetiteWeight > searchWeight) {
				
				//laziness -- beneficial for rest
				if (genes.get(LAZINESS).weight > genes.get(APPETITE).weight) {
					yHeading = node.i;
					xHeading = node.j;
				}
				else {
					//appetite trumps all -- food detected locally or nearby?
					GSEnvironmentNode neighboringNodeWithFood = null;
					int smellRadius = 5;
					boolean foodInSquare = false;
					
					//current node has food
					if (node.hasFood) {
						node.hasFood = false;
						cyclesSinceEating = 0;
						hunger -= (1 - genes.get(METABOLISM).weight);
					}
					
					//no food locally -- check neighbors within scent range
					else {
						for (int i = -smellRadius; i <= smellRadius; i++) {
							for (int j = -smellRadius; j <= smellRadius; j++) {
								if (isSafeNode(node.i + i, node.j + j)) {
									if (grid[node.i + i][node.j + j].hasFood) {
										neighboringNodeWithFood = grid[node.i + i][node.j + j];
									}
								}
							}
						}				
					}

					//calculate direction of food
					if (neighboringNodeWithFood != null) {
						
						//food vector
						int foodI = neighboringNodeWithFood.i - node.i;
						int foodJ = neighboringNodeWithFood.j - node.j;
						
						//normalize
						if (foodI != 0) {
							yHeading = foodI / Math.abs(foodI);
						}
						if (foodJ != 0) {
							xHeading = foodJ / Math.abs(foodJ);
						}
					}					
				}
			}
			
			//wander and directed search > appetite and laziness
			else {
				
				//hobo -- randomize direction
				if (genes.get(WANDER).weight > genes.get(DIRECTED_SEARCH).weight) {
					xHeading = randomDirection();
					yHeading = randomDirection();
				}
				
				//a linear thinker
				else {
					//do nothing -- xHeading, yHeading remain unchanged from last cycle
				}
			}
			
			//check validity of destination, adjust as necessary
			nextY = safeI(node.i + yHeading);
			nextX = safeJ(node.j + xHeading);
		}
		
		private boolean isSafeNode(int i, int j){
			return i >= 0 && i < gridHeight && j >= 0 && j < gridWidth;
		}
		
		private int safeI(int i){
			if (i < 0) return 0;
			if (i > gridHeight - 1) return gridHeight - 1;
			return i;
		}
		
		private int safeJ(int j){
			if (j < 0) return 0;
			if (j > gridHeight - 1) return gridHeight - 1;
			return j;
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
			double mutationProbability = 0.1;
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
			String str = "" + lifeSpan;
			for (GSAGene g : genes) {
				str += g.weight + ",";
			}
			str += "0";
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
//			System.out.println("" + weight);
		}
	}
	
	public static class GSEnvironmentNode{
		public ArrayList<GSAgent> agents = new ArrayList<GSAgent>();
		public boolean hasFood = false;
		public int i;
		public int j;
		
		public GSEnvironmentNode(int m, int n){
			i = m;
			j = n;
		}
	}
	
	public static void main(String[] args) {
		int AGENTS = 100;
		int EPOCHS = 500;
		
		//initialize with AGENTS agents
		GeneticSubsumption gs = new GeneticSubsumption(100, 100, AGENTS);
		
		//run for EPOCHS cycles
		gs.run(EPOCHS);
		
	}
}