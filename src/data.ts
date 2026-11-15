export const shops = [
  {
    id: 1,
    name: "Kopi Kita",
    category: "Café",
    area: "Jalan Dhoby",
    distance: "0.8 km",
    points: 420,
    open: true,
    image: "/images/kopi-kita.jpg",
    desc: "Good coffee. Better company. A neighbourhood café serving locally roasted beans, comforting brunches, and a little slice of home.",
    address: "18, Jalan Dhoby, Bandar Johor Bahru",
    hours: "8:00 AM – 6:00 PM",
  },
  {
    id: 2,
    name: "The Daily Knead",
    category: "Bakery",
    area: "Jalan Tan Hiok Nee",
    distance: "1.2 km",
    points: 280,
    open: true,
    image: "/images/daily-knead.jpg",
    desc: "Slow-fermented sourdough, buttery pastries, and fresh bakes made by hand every morning. Your neighbourhood’s daily bread.",
    address: "32, Jalan Tan Hiok Nee, Johor Bahru",
    hours: "7:30 AM – 5:00 PM",
  },
  {
    id: 3,
    name: "Ruang & Co.",
    category: "Lifestyle",
    area: "Jalan Trus",
    distance: "1.6 km",
    points: 180,
    open: false,
    image: "/images/ruang-co.jpg",
    desc: "Thoughtfully made things for everyday living. Discover local ceramics, homeware, and independent Malaysian makers.",
    address: "9, Jalan Trus, Bandar Johor Bahru",
    hours: "10:00 AM – 7:00 PM",
  },
  {
    id: 4,
    name: "Bunga Studio",
    category: "Retail",
    area: "Taman Mount Austin",
    distance: "3.4 km",
    points: 360,
    open: true,
    image: "/images/bunga-studio.jpg",
    desc: "Seasonal blooms and considered arrangements, made with love by your local florist.",
    address: "15, Jalan Austin Heights 8/2, Johor Bahru",
    hours: "9:00 AM – 6:00 PM",
  },
]

export type Voucher = {
  id: number
  name: string
  shop: string
  points: number
  type: string
  date: string
  requirements: string
}
export const initialVouchers: Voucher[] = [
  {
    id: 1,
    name: "A little coffee on us",
    shop: "Kopi Kita",
    points: 200,
    type: "Free beverage",
    date: "2026-12-31",
    requirements:
      "One regular hot or iced coffee. Dine-in only. One voucher per visit.",
  },
  {
    id: 2,
    name: "Your next bake, for less",
    shop: "The Daily Knead",
    points: 350,
    type: "RM5 off",
    date: "2026-12-31",
    requirements:
      "RM5 off with a minimum spend of RM15. Not valid with other promotions.",
  },
  {
    id: 3,
    name: "Make room for something lovely",
    shop: "Ruang & Co.",
    points: 600,
    type: "10% off",
    date: "2026-12-31",
    requirements: "10% off locally made homeware. Minimum purchase RM30.",
  },
  {
    id: 4,
    name: "A brighter kind of day",
    shop: "Bunga Studio",
    points: 1500,
    type: "RM20 off",
    date: "2026-12-31",
    requirements: "RM20 off a hand-tied bouquet with a minimum spend of RM80.",
  },
  {
    id: 5,
    name: "Brunch is always a good idea",
    shop: "Kopi Kita",
    points: 1800,
    type: "Free brunch",
    date: "2026-12-31",
    requirements:
      "One brunch main from the weekday menu. Available Monday to Friday.",
  },
]

export const challenges = [
  {
    title: "The neighbourhood regular",
    desc: "Visit 5 local shops this week.",
    value: 3,
    target: 5,
    reward: 100,
    icon: "shop",
    type: "Walk-in challenge",
  },
  {
    title: "A little daily ritual",
    desc: "Check in 3 days in a row.",
    value: 2,
    target: 3,
    reward: 50,
    icon: "coffee",
    type: "Streak challenge",
  },
  {
    title: "Something new, somewhere local",
    desc: "Discover 2 shops you haven’t visited.",
    value: 1,
    target: 2,
    reward: 80,
    icon: "pin",
    type: "Discovery challenge",
  },
  {
    title: "Good things come in pairs",
    desc: "Play both rewards games this week.",
    value: 1,
    target: 2,
    reward: 40,
    icon: "grid",
    type: "Game challenge",
  },
  {
    title: "Your first little reward",
    desc: "Redeem your first local voucher.",
    value: 1,
    target: 1,
    reward: 60,
    icon: "voucher",
    type: "Rewards challenge",
  },
  {
    title: "A week of local love",
    desc: "Collect 500 points this week.",
    value: 320,
    target: 500,
    reward: 150,
    icon: "heart",
    type: "Points challenge",
  },
]
