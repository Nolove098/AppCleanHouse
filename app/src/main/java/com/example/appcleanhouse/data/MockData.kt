package com.example.appcleanhouse.data

import com.example.appcleanhouse.R
import com.example.appcleanhouse.models.Cleaner
import com.example.appcleanhouse.models.Order
import com.example.appcleanhouse.models.Service

object MockData {
    val MOCK_SERVICES = listOf(
        Service(
            id = "s1",
            name = "Deep Clean",
            description = "Thorough cleaning for every corner including appliances and baseboards.",
            pricePerHour = 45,
            rating = 4.9,
            colorResId = R.drawable.bg_service_blue,
            iconResId = R.drawable.ic_sparkles
        ),
        Service(
            id = "s2",
            name = "Standard",
            description = "Regular maintenance cleaning for keeping your home fresh.",
            pricePerHour = 30,
            rating = 4.7,
            colorResId = R.drawable.bg_service_teal,
            iconResId = R.drawable.ic_warehouse
        ),
        Service(
            id = "s3",
            name = "Laundry",
            description = "Washing, drying, and folding of clothes and linens.",
            pricePerHour = 25,
            rating = 4.8,
            colorResId = R.drawable.bg_service_indigo,
            iconResId = R.drawable.laundry
        ),
        Service(
            id = "s4",
            name = "Carpet",
            description = "Deep steam cleaning to remove stains and allergens.",
            pricePerHour = 60,
            rating = 4.6,
            colorResId = R.drawable.bg_service_orange,
            iconResId = R.drawable.ic_wind
        )
    )

    val MOCK_CLEANERS = listOf(
        Cleaner(
            id = "c1",
            name = "Sarah Jenkins",
            avatarResId = R.drawable.idol1,
            rating = 4.9,
            jobCount = 142,
            specialty = "Deep Clean Specialist"
        ),
        Cleaner(
            id = "c2",
            name = "David Chen",
            avatarResId = R.drawable.idol2,
            rating = 4.8,
            jobCount = 98,
            specialty = "Fast & Efficient"
        ),
        Cleaner(
            id = "c3",
            name = "Maria Rodriguez",
            avatarResId = R.drawable.idol1,
            rating = 5.0,
            jobCount = 215,
            specialty = "Pet Friendly"
        ),
        Cleaner(
            id = "c4",
            name = "James Wilson",
            avatarResId = R.drawable.idol2,
            rating = 4.7,
            jobCount = 76,
            specialty = "Eco-Friendly Products"
        )
    )

    val MOCK_ORDERS = listOf(
        Order(
            id = "o1",
            serviceId = "s1",
            cleanerId = "c1",
            date = "Oct 24, 2023",
            time = "09:00 AM",
            status = "Upcoming",
            totalPrice = 135.00,
            address = "123 Main St, New York"
        ),
        Order(
            id = "o2",
            serviceId = "s2",
            cleanerId = "c2",
            date = "Oct 10, 2023",
            time = "02:00 PM",
            status = "Completed",
            totalPrice = 90.00,
            address = "123 Main St, New York",
            rating = 5
        ),
        Order(
            id = "o3",
            serviceId = "s3",
            cleanerId = "c3",
            date = "Sep 28, 2023",
            time = "11:00 AM",
            status = "Cancelled",
            totalPrice = 50.00,
            address = "123 Main St, New York"
        ),
        Order(
            id = "o4",
            serviceId = "s2",
            cleanerId = "c2",
            date = "Sep 15, 2023",
            time = "10:00 AM",
            status = "Completed",
            totalPrice = 90.00,
            address = "123 Main St, New York",
            rating = 4
        )
    )
}
