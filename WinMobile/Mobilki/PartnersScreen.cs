using System;
using System.Linq;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Text;
using System.Windows.Forms;

namespace Mobilki
{
    public partial class PartnersScreen : Form
    {
        private MainScreen screen;
        private PairList partners;

        public PartnersScreen(PairList pairs, MainScreen scr)
        {
            InitializeComponent();
            this.screen = scr;
            this.partners = pairs;
            foreach (int key in pairs.Keys) 
            {
                MenuItem menuItem = new MenuItem();
                menuItem.Text = (string)pairs[key];
                chooseMenuItem.MenuItems.Add(menuItem);
                menuItem.Click += new System.EventHandler(this.pairClicked);
            }
        }

        public void pairClicked(object sender, EventArgs e)
        {
            string name = ((MenuItem)sender).Text;
            foreach (int key in partners.Keys)
            {
                if (((string)partners[key]).Equals(name))
                {
                    screen.sendChoice((int)key);
                    exit(null, null);
                }
            }
        }

        public void exit(object sender, EventArgs e)
        {
            this.Hide();
            screen.Show();
        }

    }
}